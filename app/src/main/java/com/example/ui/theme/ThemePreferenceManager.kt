package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val title: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

class ThemePreferenceManager private constructor(context: Context) {

    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    private val _themeMode = MutableStateFlow(loadSavedTheme())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "app_theme_encrypted_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("ThemePreferenceManager", "Falling back to standard SharedPreferences: ${e.message}")
            context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
        }
    }

    private fun loadSavedTheme(): AppThemeMode {
        val savedName = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return try {
            AppThemeMode.valueOf(savedName ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    companion object {
        private const val KEY_THEME_MODE = "app_selected_theme_mode"

        @Volatile
        private var INSTANCE: ThemePreferenceManager? = null

        fun getInstance(context: Context): ThemePreferenceManager {
            return INSTANCE ?: synchronized(this) {
                val instance = ThemePreferenceManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
