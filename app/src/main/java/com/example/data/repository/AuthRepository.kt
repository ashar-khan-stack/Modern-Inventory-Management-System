package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.local.AppDatabase
import com.example.data.model.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

data class UserSession(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val isFingerprintEnabled: Boolean,
    val sessionToken: String = "",
    val sessionCreatedAt: Long = System.currentTimeMillis()
) {
    val fullName: String
        get() = "$firstName $lastName".trim()
}

class AuthRepository(private val context: Context, private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val prefs: SharedPreferences = createEncryptedSharedPreferences(context)

    private val _currentUserSession = MutableStateFlow<UserSession?>(null)
    val currentUserSession: StateFlow<UserSession?> = _currentUserSession.asStateFlow()

    private val _sessionExpiredEvent = MutableStateFlow(false)
    val sessionExpiredEvent: StateFlow<Boolean> = _sessionExpiredEvent.asStateFlow()

    init {
        loadSavedSession()
    }

    fun clearSessionExpiredEvent() {
        _sessionExpiredEvent.value = false
    }

    fun checkAndEnforceSessionExpiration(): Boolean {
        val current = _currentUserSession.value ?: return false
        val sessionCreatedAt = prefs.getLong(KEY_SESSION_CREATED_AT, 0L)
        val now = System.currentTimeMillis()
        if (sessionCreatedAt > 0L && (now - sessionCreatedAt) >= SESSION_DURATION_MS) {
            logout()
            _sessionExpiredEvent.value = true
            return false
        }
        return true
    }

    private fun loadSavedSession() {
        val userId = prefs.getLong(KEY_LOGGED_USER_ID, -1L)
        if (userId != -1L) {
            val sessionCreatedAt = prefs.getLong(KEY_SESSION_CREATED_AT, 0L)
            val now = System.currentTimeMillis()
            if (sessionCreatedAt > 0L && (now - sessionCreatedAt) >= SESSION_DURATION_MS) {
                logout()
                _sessionExpiredEvent.value = true
                return
            }

            val firstName = prefs.getString(KEY_USER_FIRST_NAME, "") ?: ""
            val lastName = prefs.getString(KEY_USER_LAST_NAME, "") ?: ""
            val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
            val isFingerprint = prefs.getBoolean(KEY_USER_FINGERPRINT, false)
            val token = prefs.getString(KEY_SESSION_TOKEN, "") ?: ""
            _currentUserSession.value = UserSession(
                id = userId,
                firstName = firstName,
                lastName = lastName,
                email = email,
                isFingerprintEnabled = isFingerprint,
                sessionToken = token,
                sessionCreatedAt = if (sessionCreatedAt > 0L) sessionCreatedAt else now
            )
        }
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        securityQuestions: List<Pair<String, String>>
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val cleanFirst = firstName.trim()
            val cleanLast = lastName.trim()

            val existing = userDao.getUserByEmail(cleanEmail)
            if (existing != null) {
                return@withContext Result.failure(Exception("This email is already registered. Please log in instead."))
            }

            if (securityQuestions.size != 5) {
                return@withContext Result.failure(Exception("All 5 security questions must be provided."))
            }

            val distinctQuestions = securityQuestions.map { it.first }.toSet()
            if (distinctQuestions.size != 5) {
                return@withContext Result.failure(Exception("Please select 5 different security questions."))
            }

            if (securityQuestions.any { it.second.trim().isEmpty() }) {
                return@withContext Result.failure(Exception("All 5 security question answers are required."))
            }

            val salt = generateSalt()
            val hash = hashPassword(password, salt)

            val questionItems = securityQuestions.map { (qText, rawAns) ->
                val cleanAnswer = rawAns.trim().lowercase()
                val answerHash = hashPassword(cleanAnswer, salt)
                com.example.data.model.SecurityQuestionItem(questionText = qText, answerHash = answerHash)
            }
            val questionsJson = com.example.data.model.SecurityQuestionParser.toJson(questionItems)

            val newUser = UserEntity(
                firstName = cleanFirst,
                lastName = cleanLast,
                email = cleanEmail,
                passwordHash = hash,
                salt = salt,
                isFingerprintEnabled = false,
                securityQuestionsJson = questionsJson,
                createdAt = System.currentTimeMillis()
            )

            val generatedId = userDao.insertUser(newUser)
            val sessionToken = generateSessionToken(generatedId, cleanEmail)
            val sessionCreatedAt = System.currentTimeMillis()
            val session = UserSession(
                id = generatedId,
                firstName = cleanFirst,
                lastName = cleanLast,
                email = cleanEmail,
                isFingerprintEnabled = false,
                sessionToken = sessionToken,
                sessionCreatedAt = sessionCreatedAt
            )

            saveSessionToPrefs(session)
            _currentUserSession.value = session
            Result.success(session)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun isEmailRegistered(email: String): Boolean = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        userDao.getUserByEmail(cleanEmail) != null
    }

    suspend fun login(email: String, password: String): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val user = userDao.getUserByEmail(cleanEmail)
                ?: return@withContext Result.failure(Exception("Invalid email or password. Please check your credentials."))

            val calculatedHash = hashPassword(password, user.salt)
            if (calculatedHash != user.passwordHash) {
                return@withContext Result.failure(Exception("Invalid email or password. Please check your credentials."))
            }

            val sessionToken = generateSessionToken(user.id, user.email)
            val sessionCreatedAt = System.currentTimeMillis()
            val session = UserSession(
                id = user.id,
                firstName = user.firstName,
                lastName = user.lastName,
                email = user.email,
                isFingerprintEnabled = user.isFingerprintEnabled,
                sessionToken = sessionToken,
                sessionCreatedAt = sessionCreatedAt
            )

            saveSessionToPrefs(session)
            _currentUserSession.value = session
            Result.success(session)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun loginWithBiometric(emailHint: String? = null): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            var user: UserEntity? = null

            if (!emailHint.isNullOrBlank()) {
                user = userDao.getUserByEmail(emailHint.trim().lowercase())
            }

            if (user == null) {
                val lastUserId = prefs.getLong(KEY_LAST_BIOMETRIC_USER_ID, -1L)
                if (lastUserId != -1L) {
                    user = userDao.getUserById(lastUserId)
                }
            }

            if (user == null) {
                val loggedUserId = prefs.getLong(KEY_LOGGED_USER_ID, -1L)
                if (loggedUserId != -1L) {
                    user = userDao.getUserById(loggedUserId)
                }
            }

            if (user == null) {
                val savedEmail = prefs.getString(KEY_USER_EMAIL, null)
                if (!savedEmail.isNullOrBlank()) {
                    user = userDao.getUserByEmail(savedEmail)
                }
            }

            if (user == null) {
                return@withContext Result.failure(
                    Exception("No account registered or previously enrolled on this device. Please create an account or sign in with your email & password first.")
                )
            }

            val sessionToken = generateSessionToken(user.id, user.email)
            val sessionCreatedAt = System.currentTimeMillis()
            val session = UserSession(
                id = user.id,
                firstName = user.firstName,
                lastName = user.lastName,
                email = user.email,
                isFingerprintEnabled = true,
                sessionToken = sessionToken,
                sessionCreatedAt = sessionCreatedAt
            )

            userDao.setFingerprintEnabled(user.id, true)
            saveSessionToPrefs(session)
            _currentUserSession.value = session
            Result.success(session)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun findUserByEmail(email: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val user = userDao.getUserByEmail(cleanEmail)
                ?: return@withContext Result.failure(Exception("No account registered with email '$cleanEmail'."))
            Result.success(user)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun verifySecurityAnswer(user: UserEntity, questionText: String, userAnswer: String): Boolean {
        val cleanAnswer = userAnswer.trim().lowercase()
        val calculatedHash = hashPassword(cleanAnswer, user.salt)
        val items = com.example.data.model.SecurityQuestionParser.jsonToList(user.securityQuestionsJson)
        val item = items.find { it.questionText == questionText } ?: return false
        return item.answerHash == calculatedHash
    }

    fun verifySecurityAnswers(user: UserEntity, userAnswers: List<String>): Boolean {
        val items = com.example.data.model.SecurityQuestionParser.jsonToList(user.securityQuestionsJson)
        if (items.isEmpty()) return false
        if (userAnswers.size != items.size) return false
        for (i in items.indices) {
            val cleanAnswer = userAnswers[i].trim().lowercase()
            val calculatedHash = hashPassword(cleanAnswer, user.salt)
            if (calculatedHash != items[i].answerHash) {
                return false
            }
        }
        return true
    }

    suspend fun resetPassword(email: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val user = userDao.getUserByEmail(cleanEmail)
                ?: return@withContext Result.failure(Exception("No account registered with email '$cleanEmail'."))

            val newSalt = generateSalt()
            val newHash = hashPassword(newPassword, newSalt)
            userDao.updatePassword(cleanEmail, newHash, newSalt)

            logout()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun setFingerprintEnabled(enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = _currentUserSession.value
                ?: return@withContext Result.failure(Exception("No active user session."))

            userDao.setFingerprintEnabled(current.id, enabled)
            val updated = current.copy(isFingerprintEnabled = enabled)
            _currentUserSession.value = updated
            saveSessionToPrefs(updated)

            if (enabled) {
                prefs.edit().putLong(KEY_LAST_BIOMETRIC_USER_ID, current.id).apply()
            } else {
                prefs.edit().remove(KEY_LAST_BIOMETRIC_USER_ID).apply()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun hasBiometricUserEnrolled(): Boolean {
        val lastUserId = prefs.getLong(KEY_LAST_BIOMETRIC_USER_ID, -1L)
        val loggedUserId = prefs.getLong(KEY_LOGGED_USER_ID, -1L)
        val savedEmail = prefs.getString(KEY_USER_EMAIL, null)
        return lastUserId != -1L || loggedUserId != -1L || !savedEmail.isNullOrBlank()
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_LOGGED_USER_ID)
            .remove(KEY_USER_FIRST_NAME)
            .remove(KEY_USER_LAST_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_FINGERPRINT)
            .remove(KEY_SESSION_TOKEN)
            .remove(KEY_SESSION_CREATED_AT)
            .apply()
        _currentUserSession.value = null
    }

    private fun saveSessionToPrefs(session: UserSession) {
        prefs.edit()
            .putLong(KEY_LOGGED_USER_ID, session.id)
            .putString(KEY_USER_FIRST_NAME, session.firstName)
            .putString(KEY_USER_LAST_NAME, session.lastName)
            .putString(KEY_USER_EMAIL, session.email)
            .putBoolean(KEY_USER_FINGERPRINT, session.isFingerprintEnabled)
            .putString(KEY_SESSION_TOKEN, session.sessionToken)
            .putLong(KEY_SESSION_CREATED_AT, session.sessionCreatedAt)
            .apply()

        if (session.isFingerprintEnabled) {
            prefs.edit().putLong(KEY_LAST_BIOMETRIC_USER_ID, session.id).apply()
        }
    }

    private fun generateSessionToken(userId: Long, email: String): String {
        val raw = "$userId:$email:${UUID.randomUUID()}:${System.currentTimeMillis()}"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(raw.toByteArray())
        return Base64.getEncoder().encodeToString(digest)
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return Base64.getEncoder().encodeToString(saltBytes)
    }

    private fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray())
        val hashedBytes = md.digest(password.toByteArray())
        return Base64.getEncoder().encodeToString(hashedBytes)
    }

    companion object {
        val PREDEFINED_SECURITY_QUESTIONS = listOf(
            "What was the name of your first school?",
            "What was your childhood nickname?",
            "What was the name of your favorite childhood teacher?",
            "What was the name of your first pet?",
            "What was your favorite childhood game?"
        )

        private const val SESSION_DURATION_MS = 30L * 24 * 60 * 60 * 1000L // 30 days
        private const val KEY_LOGGED_USER_ID = "logged_user_id"
        private const val KEY_USER_FIRST_NAME = "user_first_name"
        private const val KEY_USER_LAST_NAME = "user_last_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_FINGERPRINT = "user_fingerprint"
        private const val KEY_LAST_BIOMETRIC_USER_ID = "last_biometric_user_id"
        private const val KEY_SESSION_TOKEN = "user_session_token"
        private const val KEY_SESSION_CREATED_AT = "session_created_at"

        private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    "app_auth_encrypted_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.w("AuthRepository", "EncryptedSharedPreferences init fallback: ${e.message}")
                context.getSharedPreferences("app_auth_prefs", Context.MODE_PRIVATE)
            }
        }

        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val instance = AuthRepository(context.applicationContext, db)
                INSTANCE = instance
                instance
            }
        }
    }
}
