package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserSession
import com.example.ui.theme.*
import com.example.ui.util.BiometricHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    onAuthSuccess: (UserSession) -> Unit,
    onLaunchBiometricPrompt: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var isLoginTab by remember { mutableStateOf(true) }

    // Login Fields
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Register Fields
    var regFirstName by remember { mutableStateOf("") }
    var regLastName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regConfirmPasswordVisible by remember { mutableStateOf(false) }

    // Touched tracking for Sign Up form fields
    var firstNameTouched by remember { mutableStateOf(false) }
    var lastNameTouched by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    var confirmPasswordTouched by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val isBiometricHardwareAvailable = remember {
        BiometricHelper.isBiometricAvailable(context)
    }

    // --- REAL-TIME VALIDATION LOGIC ---
    val emailRegex = remember { Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") }
    val nameLetterOnlyRegex = remember { Regex("^[a-zA-ZÀ-ÿ' -]+$") }

    // 1. First Name Validation
    val firstNameTrimmed = regFirstName.trim()
    val (isFirstNameValid, firstNameError) = remember(regFirstName) {
        when {
            regFirstName.isEmpty() -> Pair(false, "First name is required.")
            !nameLetterOnlyRegex.matches(regFirstName) -> Pair(false, "Letters only (no numbers or symbols).")
            firstNameTrimmed.length < 2 -> Pair(false, "Must be at least 2 characters.")
            else -> Pair(true, null)
        }
    }

    // 2. Last Name Validation
    val lastNameTrimmed = regLastName.trim()
    val (isLastNameValid, lastNameError) = remember(regLastName) {
        when {
            regLastName.isEmpty() -> Pair(false, "Last name is required.")
            !nameLetterOnlyRegex.matches(regLastName) -> Pair(false, "Letters only (no numbers or symbols).")
            lastNameTrimmed.length < 2 -> Pair(false, "Must be at least 2 characters.")
            else -> Pair(true, null)
        }
    }

    // 3. Email Validation & Duplicate Check
    var isEmailRegistered by remember { mutableStateOf(false) }

    LaunchedEffect(regEmail) {
        val trimmed = regEmail.trim().lowercase()
        if (emailRegex.matches(trimmed)) {
            kotlinx.coroutines.delay(200)
            isEmailRegistered = authRepository.isEmailRegistered(trimmed)
        } else {
            isEmailRegistered = false
        }
    }

    val emailTrimmed = regEmail.trim()
    val emailFormatValid = emailRegex.matches(emailTrimmed)
    val isEmailValid = emailFormatValid && !isEmailRegistered

    val emailError = when {
        emailTrimmed.isEmpty() -> "Email address is required."
        !emailFormatValid -> {
            if (!emailTrimmed.contains("@")) {
                "Please include '@' in the email address (e.g. user@domain.com)."
            } else if (emailTrimmed.endsWith("@") || !emailTrimmed.substringAfter("@").contains(".")) {
                "Please include a valid domain (e.g. domain.com)."
            } else {
                "Please enter a valid email address (e.g. name@domain.com)."
            }
        }
        isEmailRegistered -> "This email is already registered. Please log in instead."
        else -> null
    }

    // 4. Password Policy Live Requirements
    val hasMinLength = regPassword.length >= 8
    val hasUppercase = regPassword.any { it.isUpperCase() }
    val hasLowercase = regPassword.any { it.isLowerCase() }
    val hasDigit = regPassword.any { it.isDigit() }
    val hasSpecial = regPassword.any { !it.isLetterOrDigit() }

    val isPasswordValid = hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecial

    // 5. Confirm Password Validation (Live Cross-Field with Password)
    val (isConfirmPasswordValid, confirmPasswordError) = remember(regPassword, regConfirmPassword) {
        when {
            regConfirmPassword.isEmpty() -> Pair(false, "Please confirm your password.")
            regConfirmPassword != regPassword -> Pair(false, "Passwords do not match.")
            !isPasswordValid -> Pair(false, "Password does not meet all requirements.")
            else -> Pair(true, null)
        }
    }

    val sessionExpired by authRepository.sessionExpiredEvent.collectAsState()

    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            errorMessage = "Your session has expired. Please log in again."
            authRepository.clearSessionExpiredEvent()
        }
    }

    // Security Questions Fields for Register
    val questionsList = AuthRepository.PREDEFINED_SECURITY_QUESTIONS
    var q0 by remember { mutableStateOf(questionsList[0]) }
    var q1 by remember { mutableStateOf(questionsList[1]) }
    var q2 by remember { mutableStateOf(questionsList[2]) }
    var q3 by remember { mutableStateOf(questionsList[3]) }
    var q4 by remember { mutableStateOf(questionsList[4]) }

    var ans0 by remember { mutableStateOf("") }
    var ans1 by remember { mutableStateOf("") }
    var ans2 by remember { mutableStateOf("") }
    var ans3 by remember { mutableStateOf("") }
    var ans4 by remember { mutableStateOf("") }

    var securityQuestionsTouched by remember { mutableStateOf(false) }

    val selectedQuestionsSet = remember(q0, q1, q2, q3, q4) {
        listOf(q0, q1, q2, q3, q4).toSet()
    }
    val isQuestionsUnique = selectedQuestionsSet.size == 5
    val isAnswersProvided = ans0.trim().isNotEmpty() &&
            ans1.trim().isNotEmpty() &&
            ans2.trim().isNotEmpty() &&
            ans3.trim().isNotEmpty() &&
            ans4.trim().isNotEmpty()

    val isSecurityQuestionsValid = isQuestionsUnique && isAnswersProvided

    val isAllSignUpValid = isFirstNameValid && isLastNameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid && isSecurityQuestionsValid

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ==========================================
            // HEADER & BRANDING SECTION
            // ==========================================
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(BrandBluePrimary, BrandNavySecondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = "App Logo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isLoginTab) "Welcome Back" else "Create Your Account",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isLoginTab) "Sign in to access your inventory and sales records." else "Sign up to get started with your account.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // MAIN CARD CONTAINER
            // ==========================================
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp)
                ) {
                    // Segmented Tab Switcher
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isLoginTab) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isLoginTab) 2.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        isLoginTab = true
                                        errorMessage = null
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Sign In",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (isLoginTab) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isLoginTab) BrandBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (!isLoginTab) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (!isLoginTab) 2.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        isLoginTab = false
                                        errorMessage = null
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Create Account",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (!isLoginTab) FontWeight.Bold else FontWeight.Medium,
                                            color = if (!isLoginTab) BrandBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error Message Banner
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        errorMessage?.let { error ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DangerRedContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = "Error icon",
                                        tint = DangerRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = DangerRed,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (isLoginTab) {
                        // ==========================================
                        // --- LOGIN FORM ---
                        // ==========================================
                        Text(
                            text = "Email Address",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = {
                                loginEmail = it
                                errorMessage = null
                            },
                            placeholder = { Text("Enter your email (e.g. name@domain.com)") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = "Email icon",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Password",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = {
                                loginPassword = it
                                errorMessage = null
                            },
                            placeholder = { Text("Enter your password") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Password icon",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { loginPasswordVisible = !loginPasswordVisible },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (loginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (loginPasswordVisible) "Hide password" else "Show password",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Forgot Password Link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showForgotPasswordDialog = true },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BrandBluePrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sign In Button
                        Button(
                            onClick = {
                                if (loginEmail.isBlank()) {
                                    errorMessage = "Please enter your email address."
                                    return@Button
                                }
                                if (!emailRegex.matches(loginEmail.trim())) {
                                    errorMessage = "Please enter a valid email address."
                                    return@Button
                                }
                                if (loginPassword.isBlank()) {
                                    errorMessage = "Please enter your password."
                                    return@Button
                                }

                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    val result = authRepository.login(loginEmail, loginPassword)
                                    isLoading = false
                                    if (result.isSuccess) {
                                        val session = result.getOrThrow()
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { Toast.makeText(context, "Welcome back, ${session.firstName}!", Toast.LENGTH_SHORT).show() }
                                        onAuthSuccess(session)
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message ?: "Invalid credentials. Please try again."
                                    }
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_submit_button")
                        ) {
                            if (isLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Signing In...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            } else {
                                Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        // Biometric Sign In (Fingerprint or Face Scan)
                        if (isBiometricHardwareAvailable) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "OR",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    errorMessage = null
                                    onLaunchBiometricPrompt(
                                        {
                                            scope.launch {
                                                isLoading = true
                                                val result = authRepository.loginWithBiometric(
                                                    emailHint = loginEmail.ifBlank { null }
                                                )
                                                isLoading = false
                                                if (result.isSuccess) {
                                                    val session = result.getOrThrow()
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        Toast.makeText(
                                                            context,
                                                            "Biometric verified! Welcome, ${session.firstName}!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                    onAuthSuccess(session)
                                                } else {
                                                    errorMessage = result.exceptionOrNull()?.message
                                                        ?: "Biometric authentication failed. Please sign in with email & password."
                                                }
                                            }
                                        },
                                        { err ->
                                            errorMessage = err
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, BrandBluePrimary.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = BrandBluePrimary.copy(alpha = 0.04f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("biometric_login_button")
                            ) {
                                Icon(
                                    Icons.Default.Fingerprint,
                                    contentDescription = "Biometric Scanner",
                                    tint = BrandBluePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Sign in with Fingerprint / Face Scan",
                                    color = BrandBluePrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Link to Switch to Sign Up
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Don't have an account?",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sign Up",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = BrandBluePrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .clickable {
                                        isLoginTab = false
                                        errorMessage = null
                                    }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            )
                        }

                    } else {
                        // ==========================================
                        // --- SIGN UP / CREATE ACCOUNT FORM ---
                        // ==========================================
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isWide = maxWidth >= 400.dp
                            if (isWide) {
                                // Side-by-side on wide screens / tablets
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // First Name
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "First Name",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        OutlinedTextField(
                                            value = regFirstName,
                                            onValueChange = { input ->
                                                regFirstName = input.trimStart()
                                                firstNameTouched = true
                                                errorMessage = null
                                            },
                                            placeholder = { Text("First name") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = "First name icon",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            trailingIcon = {
                                                if (firstNameTouched) {
                                                    if (isFirstNameValid) {
                                                        Icon(
                                                            Icons.Default.CheckCircle,
                                                            contentDescription = "Valid first name",
                                                            tint = SuccessGreen,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    } else {
                                                        Icon(
                                                            Icons.Default.ErrorOutline,
                                                            contentDescription = "Invalid first name",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            isError = firstNameTouched && !isFirstNameValid,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Text,
                                                imeAction = ImeAction.Next
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onNext = {
                                                    firstNameTouched = true
                                                    focusManager.moveFocus(FocusDirection.Right)
                                                }
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onFocusChanged { if (!it.isFocused && regFirstName.isNotEmpty()) firstNameTouched = true }
                                                .testTag("reg_first_name_input")
                                        )
                                        if (firstNameTouched && !isFirstNameValid && firstNameError != null) {
                                            Text(
                                                text = firstNameError,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                modifier = Modifier.padding(start = 4.dp, top = 3.dp)
                                            )
                                        }
                                    }

                                    // Last Name
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Last Name",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        OutlinedTextField(
                                            value = regLastName,
                                            onValueChange = { input ->
                                                regLastName = input.trimStart()
                                                lastNameTouched = true
                                                errorMessage = null
                                            },
                                            placeholder = { Text("Last name") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.PersonOutline,
                                                    contentDescription = "Last name icon",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            trailingIcon = {
                                                if (lastNameTouched) {
                                                    if (isLastNameValid) {
                                                        Icon(
                                                            Icons.Default.CheckCircle,
                                                            contentDescription = "Valid last name",
                                                            tint = SuccessGreen,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    } else {
                                                        Icon(
                                                            Icons.Default.ErrorOutline,
                                                            contentDescription = "Invalid last name",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            isError = lastNameTouched && !isLastNameValid,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Text,
                                                imeAction = ImeAction.Next
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onNext = {
                                                    lastNameTouched = true
                                                    focusManager.moveFocus(FocusDirection.Down)
                                                }
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onFocusChanged { if (!it.isFocused && regLastName.isNotEmpty()) lastNameTouched = true }
                                                .testTag("reg_last_name_input")
                                        )
                                        if (lastNameTouched && !isLastNameValid && lastNameError != null) {
                                            Text(
                                                text = lastNameError,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                modifier = Modifier.padding(start = 4.dp, top = 3.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Stacked vertically on standard phones
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // First Name
                                    Text(
                                        text = "First Name",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    OutlinedTextField(
                                        value = regFirstName,
                                        onValueChange = { input ->
                                            regFirstName = input.trimStart()
                                            firstNameTouched = true
                                            errorMessage = null
                                        },
                                        placeholder = { Text("Enter first name") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = "First name icon",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingIcon = {
                                            if (firstNameTouched) {
                                                if (isFirstNameValid) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = "Valid first name",
                                                        tint = SuccessGreen,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Default.ErrorOutline,
                                                        contentDescription = "Invalid first name",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        isError = firstNameTouched && !isFirstNameValid,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = {
                                                firstNameTouched = true
                                                focusManager.moveFocus(FocusDirection.Down)
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { if (!it.isFocused && regFirstName.isNotEmpty()) firstNameTouched = true }
                                            .testTag("reg_first_name_input")
                                    )
                                    if (firstNameTouched && !isFirstNameValid && firstNameError != null) {
                                        Text(
                                            text = firstNameError,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            modifier = Modifier.padding(start = 4.dp, top = 3.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Last Name
                                    Text(
                                        text = "Last Name",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    OutlinedTextField(
                                        value = regLastName,
                                        onValueChange = { input ->
                                            regLastName = input.trimStart()
                                            lastNameTouched = true
                                            errorMessage = null
                                        },
                                        placeholder = { Text("Enter last name") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.PersonOutline,
                                                contentDescription = "Last name icon",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingIcon = {
                                            if (lastNameTouched) {
                                                if (isLastNameValid) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = "Valid last name",
                                                        tint = SuccessGreen,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Default.ErrorOutline,
                                                        contentDescription = "Invalid last name",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        isError = lastNameTouched && !isLastNameValid,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = {
                                                lastNameTouched = true
                                                focusManager.moveFocus(FocusDirection.Down)
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { if (!it.isFocused && regLastName.isNotEmpty()) lastNameTouched = true }
                                            .testTag("reg_last_name_input")
                                    )
                                    if (lastNameTouched && !isLastNameValid && lastNameError != null) {
                                        Text(
                                            text = lastNameError,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            modifier = Modifier.padding(start = 4.dp, top = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. EMAIL ADDRESS
                        Text(
                            text = "Email",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { input ->
                                regEmail = input
                                emailTouched = true
                                errorMessage = null
                            },
                            placeholder = { Text("Enter your mail") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = "Email icon",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (emailTouched) {
                                    if (isEmailValid) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Valid email address",
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.ErrorOutline,
                                            contentDescription = "Invalid email address",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            isError = emailTouched && !isEmailValid,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    emailTouched = true
                                    focusManager.moveFocus(FocusDirection.Down)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { if (!it.isFocused && regEmail.isNotEmpty()) emailTouched = true }
                                .testTag("reg_email_input")
                        )
                        if (emailTouched && emailError != null) {
                            if (isEmailRegistered) {
                                Row(
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "This email is already registered. ",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = "Please log in instead.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BrandBluePrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.clickable {
                                            isLoginTab = true
                                            loginEmail = regEmail.trim()
                                            errorMessage = null
                                        }
                                    )
                                }
                            } else {
                                Text(
                                    text = emailError,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.padding(start = 4.dp, top = 3.dp)
                                )
                            }
                        } else if (emailTouched && isEmailValid) {
                            Text(
                                text = "Email is available.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = SuccessGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(start = 4.dp, top = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. PASSWORD
                        Text(
                            text = "Password",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { input ->
                                regPassword = input
                                passwordTouched = true
                                errorMessage = null
                            },
                            placeholder = { Text("Create password") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Password lock icon",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    if (passwordTouched) {
                                        if (isPasswordValid) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Valid password",
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.ErrorOutline,
                                                contentDescription = "Password requirements not met",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    IconButton(
                                        onClick = { regPasswordVisible = !regPasswordVisible },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (regPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (regPasswordVisible) "Hide password" else "Show password",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            isError = passwordTouched && !isPasswordValid,
                            visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    passwordTouched = true
                                    focusManager.moveFocus(FocusDirection.Down)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { if (!it.isFocused && regPassword.isNotEmpty()) passwordTouched = true }
                                .testTag("reg_password_input")
                        )

                        // Compact Password Requirements Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(
                                    text = "Password requirements",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                PasswordRequirementItem(
                                    label = "8+ characters",
                                    isMet = hasMinLength,
                                    isTouched = passwordTouched
                                )
                                PasswordRequirementItem(
                                    label = "Uppercase letter (A-Z)",
                                    isMet = hasUppercase,
                                    isTouched = passwordTouched
                                )
                                PasswordRequirementItem(
                                    label = "Lowercase letter (a-z)",
                                    isMet = hasLowercase,
                                    isTouched = passwordTouched
                                )
                                PasswordRequirementItem(
                                    label = "Number (0-9)",
                                    isMet = hasDigit,
                                    isTouched = passwordTouched
                                )
                                PasswordRequirementItem(
                                    label = "Special character (!@#\$%...)",
                                    isMet = hasSpecial,
                                    isTouched = passwordTouched
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 5. CONFIRM PASSWORD
                        Text(
                            text = "Confirm Password",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = regConfirmPassword,
                            onValueChange = { input ->
                                regConfirmPassword = input
                                confirmPasswordTouched = true
                                errorMessage = null
                            },
                            placeholder = { Text("Confirm your password") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = "Confirm password icon",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    if (confirmPasswordTouched) {
                                        if (isConfirmPasswordValid) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Passwords match",
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.ErrorOutline,
                                                contentDescription = "Passwords do not match",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    IconButton(
                                        onClick = { regConfirmPasswordVisible = !regConfirmPasswordVisible },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (regConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (regConfirmPasswordVisible) "Hide password" else "Show password",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            isError = confirmPasswordTouched && !isConfirmPasswordValid,
                            visualTransformation = if (regConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    confirmPasswordTouched = true
                                    focusManager.clearFocus()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { if (!it.isFocused && regConfirmPassword.isNotEmpty()) confirmPasswordTouched = true }
                                .testTag("reg_confirm_password_input")
                        )

                        if (confirmPasswordTouched && !isConfirmPasswordValid && confirmPasswordError != null) {
                            Text(
                                text = confirmPasswordError,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(start = 4.dp, top = 3.dp)
                            )
                        } else if (confirmPasswordTouched && isConfirmPasswordValid) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 4.dp, top = 3.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Passwords match",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = SuccessGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        // ==========================================
                        // --- SECURITY QUESTIONS SECTION ---
                        // ==========================================
                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Security Questions",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Set all 5 questions to help recover your account if you forget your password.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (securityQuestionsTouched && !isQuestionsUnique) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DangerRedContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = DangerRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Please select 5 different security questions. Duplicate question selected.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = DangerRed,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }

                        val questionsState = listOf(
                            Triple(q0, ans0, { newQ: String -> q0 = newQ } to { newA: String -> ans0 = newA }),
                            Triple(q1, ans1, { newQ: String -> q1 = newQ } to { newA: String -> ans1 = newA }),
                            Triple(q2, ans2, { newQ: String -> q2 = newQ } to { newA: String -> ans2 = newA }),
                            Triple(q3, ans3, { newQ: String -> q3 = newQ } to { newA: String -> ans3 = newA }),
                            Triple(q4, ans4, { newQ: String -> q4 = newQ } to { newA: String -> ans4 = newA })
                        )

                        questionsState.forEachIndexed { index, (selectedQ, answer, handlers) ->
                            val (onQChange, onAChange) = handlers
                            SecurityQuestionInputBlock(
                                index = index,
                                selectedQuestion = selectedQ,
                                allQuestions = questionsList,
                                answer = answer,
                                onQuestionSelected = { newQ -> onQChange(newQ); securityQuestionsTouched = true },
                                onAnswerChanged = { newA -> onAChange(newA); securityQuestionsTouched = true },
                                isTouched = securityQuestionsTouched
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 6. CREATE ACCOUNT BUTTON
                        Button(
                            onClick = {
                                // Mark all fields as touched for final submission check
                                firstNameTouched = true
                                lastNameTouched = true
                                emailTouched = true
                                passwordTouched = true
                                confirmPasswordTouched = true
                                securityQuestionsTouched = true

                                if (!isAllSignUpValid) {
                                    errorMessage = when {
                                        !isFirstNameValid -> firstNameError ?: "Please enter a valid first name."
                                        !isLastNameValid -> lastNameError ?: "Please enter a valid last name."
                                        !isEmailValid -> emailError ?: "Please enter a valid email address."
                                        !isPasswordValid -> "Password must meet all 5 requirements listed."
                                        !isConfirmPasswordValid -> confirmPasswordError ?: "Passwords do not match."
                                        !isQuestionsUnique -> "Please select 5 different security questions."
                                        !isAnswersProvided -> "Please provide answers for all 5 security questions."
                                        else -> "Please fill in all fields correctly."
                                    }
                                    return@Button
                                }

                                val cleanFirst = firstNameTrimmed
                                val cleanLast = lastNameTrimmed
                                val cleanEmail = emailTrimmed
                                val secQuestions = listOf(
                                    Pair(q0, ans0),
                                    Pair(q1, ans1),
                                    Pair(q2, ans2),
                                    Pair(q3, ans3),
                                    Pair(q4, ans4)
                                )

                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    val result = authRepository.register(cleanFirst, cleanLast, cleanEmail, regPassword, secQuestions)
                                    isLoading = false
                                    if (result.isSuccess) {
                                        val session = result.getOrThrow()
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { Toast.makeText(context, "Registration successful! Welcome, ${session.firstName}!", Toast.LENGTH_LONG).show() }
                                        onAuthSuccess(session)
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message ?: "Registration failed."
                                    }
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("reg_submit_button")
                        ) {
                            if (isLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Creating Account...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            } else {
                                Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        // Bottom Link: Already have an account? Log In
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Already have an account?",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Log In",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = BrandBluePrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .clickable {
                                        isLoginTab = true
                                        errorMessage = null
                                    }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Encrypted at rest with AES-256 local keystore & biometric auth",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Forgot Password Dialog
        if (showForgotPasswordDialog) {
            ForgotPasswordDialog(
                initialEmail = loginEmail,
                authRepository = authRepository,
                onDismiss = { showForgotPasswordDialog = false },
                onPasswordResetSuccess = { resetEmail ->
                    loginEmail = resetEmail
                    loginPassword = ""
                    showForgotPasswordDialog = false
                    Toast.makeText(context, "Password updated! Please log in with your new password.", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityQuestionInputBlock(
    index: Int,
    selectedQuestion: String,
    allQuestions: List<String>,
    answer: String,
    onQuestionSelected: (String) -> Unit,
    onAnswerChanged: (String) -> Unit,
    isTouched: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Security Question ${index + 1}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BrandBluePrimary
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedQuestion,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("reg_security_question_dropdown_$index")
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    allQuestions.forEach { question ->
                        DropdownMenuItem(
                            text = { Text(question, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onQuestionSelected(question)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Answer:",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = answer,
                onValueChange = onAnswerChanged,
                placeholder = { Text("Enter your answer") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                isError = isTouched && answer.trim().isEmpty(),
                supportingText = if (isTouched && answer.trim().isEmpty()) {
                    { Text("Answer is required", color = MaterialTheme.colorScheme.error) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reg_security_answer_input_$index")
            )
        }
    }
}

@Composable
fun ForgotPasswordDialog(
    initialEmail: String,
    authRepository: AuthRepository,
    onDismiss: () -> Unit,
    onPasswordResetSuccess: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableIntStateOf(1) } // 1: Identify, 2: Security Question, 3: Password Form, 4: Success

    var identifierInput by remember { mutableStateOf(initialEmail) }
    var foundUser by remember { mutableStateOf<com.example.data.model.UserEntity?>(null) }
    var selectedQuestionItem by remember { mutableStateOf<com.example.data.model.SecurityQuestionItem?>(null) }

    var answerInput by remember { mutableStateOf("") }
    var failedAttemptsCount by remember { mutableIntStateOf(0) }
    var isAttemptsBlocked by remember { mutableStateOf(false) }

    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmNewPasswordVisible by remember { mutableStateOf(false) }

    var isResetLoading by remember { mutableStateOf(false) }
    var dialogErrorMessage by remember { mutableStateOf<String?>(null) }

    val emailRegex = remember { Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") }

    val hasMinLength = newPassword.length >= 8
    val hasUppercase = newPassword.any { it.isUpperCase() }
    val hasLowercase = newPassword.any { it.isLowerCase() }
    val hasDigit = newPassword.any { it.isDigit() }
    val hasSpecial = newPassword.any { !it.isLetterOrDigit() }
    val isPasswordValid = hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecial
    val isPasswordMatch = newPassword.isNotEmpty() && newPassword == confirmNewPassword

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BrandBluePrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentStep == 4) Icons.Default.CheckCircle else Icons.Default.LockReset,
                        contentDescription = null,
                        tint = if (currentStep == 4) SuccessGreen else BrandBluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when (currentStep) {
                        1 -> "Account Recovery"
                        2 -> "Security Question"
                        3 -> "Create New Password"
                        else -> "Password Changed!"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (dialogErrorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DangerRedContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dialogErrorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(color = DangerRed)
                            )
                        }
                    }
                }

                when (currentStep) {
                    1 -> {
                        Text(
                            text = "Enter your registered email address to begin account recovery.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = identifierInput,
                            onValueChange = {
                                identifierInput = it
                                dialogErrorMessage = null
                            },
                            label = { Text("Account Email") },
                            placeholder = { Text("name@example.com") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_email_input")
                        )
                    }
                    2 -> {
                        Text(
                            text = "Answer the security question configured for your account:",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Security Question",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BrandBluePrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedQuestionItem?.questionText ?: "",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.testTag("forgot_question_text")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = answerInput,
                            onValueChange = {
                                answerInput = it
                                dialogErrorMessage = null
                            },
                            label = { Text("Your Answer") },
                            placeholder = { Text("Enter answer") },
                            leadingIcon = { Icon(Icons.Default.Help, contentDescription = null) },
                            singleLine = true,
                            enabled = !isAttemptsBlocked,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_answer_input")
                        )

                        if (failedAttemptsCount > 0 && !isAttemptsBlocked) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Attempts remaining: ${5 - failedAttemptsCount}",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error)
                            )
                        }
                    }
                    3 -> {
                        Text(
                            text = "Security question verified successfully! Create a new password for ${foundUser?.email}.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                dialogErrorMessage = null
                            },
                            label = { Text("New Password") },
                            placeholder = { Text("Enter new password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(
                                        imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_new_password_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = {
                                confirmNewPassword = it
                                dialogErrorMessage = null
                            },
                            label = { Text("Confirm New Password") },
                            placeholder = { Text("Re-enter new password") },
                            leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { confirmNewPasswordVisible = !confirmNewPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (confirmNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = confirmNewPassword.isNotEmpty() && !isPasswordMatch,
                            supportingText = if (confirmNewPassword.isNotEmpty() && !isPasswordMatch) {
                                { Text("Passwords do not match.", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_confirm_password_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Requirements:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(4.dp))
                                PasswordRequirementItem("At least 8 characters", hasMinLength, newPassword.isNotEmpty())
                                PasswordRequirementItem("Uppercase & lowercase letters", hasUppercase && hasLowercase, newPassword.isNotEmpty())
                                PasswordRequirementItem("At least 1 number", hasDigit, newPassword.isNotEmpty())
                                PasswordRequirementItem("At least 1 special symbol", hasSpecial, newPassword.isNotEmpty())
                            }
                        }
                    }
                    4 -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Password Changed Successfully.",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your account password has been updated. You must now log in using your new password.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (currentStep) {
                1 -> {
                    Button(
                        onClick = {
                            val clean = identifierInput.trim().lowercase()
                            if (clean.isBlank()) {
                                dialogErrorMessage = "Please enter your email."
                                return@Button
                            }
                            if (!emailRegex.matches(clean)) {
                                dialogErrorMessage = "Please enter a valid email address."
                                return@Button
                            }

                            isResetLoading = true
                            dialogErrorMessage = null
                            scope.launch {
                                val result = authRepository.findUserByEmail(clean)
                                isResetLoading = false
                                if (result.isSuccess) {
                                    val user = result.getOrThrow()
                                    val questions = com.example.data.model.SecurityQuestionParser.jsonToList(user.securityQuestionsJson)
                                    if (questions.isEmpty()) {
                                        dialogErrorMessage = "Account recovery is not configured for this account. Please log in with your password."
                                    } else {
                                        foundUser = user
                                        selectedQuestionItem = questions.random()
                                        failedAttemptsCount = 0
                                        isAttemptsBlocked = false
                                        answerInput = ""
                                        currentStep = 2
                                    }
                                } else {
                                    dialogErrorMessage = result.exceptionOrNull()?.message ?: "Account not found."
                                }
                            }
                        },
                        enabled = !isResetLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        modifier = Modifier.testTag("forgot_verify_email_button")
                    ) {
                        if (isResetLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Identify Account")
                        }
                    }
                }
                2 -> {
                    Button(
                        onClick = {
                            if (isAttemptsBlocked) {
                                dialogErrorMessage = "Maximum failed attempts reached. Recovery temporarily locked."
                                return@Button
                            }
                            if (answerInput.trim().isEmpty()) {
                                dialogErrorMessage = "Please enter an answer."
                                return@Button
                            }

                            val user = foundUser
                            val question = selectedQuestionItem
                            if (user == null || question == null) {
                                dialogErrorMessage = "An error occurred. Please restart the recovery process."
                                return@Button
                            }

                            val isCorrect = authRepository.verifySecurityAnswer(user, question.questionText, answerInput)
                            if (isCorrect) {
                                dialogErrorMessage = null
                                currentStep = 3
                            } else {
                                failedAttemptsCount++
                                if (failedAttemptsCount >= 5) {
                                    isAttemptsBlocked = true
                                    dialogErrorMessage = "Incorrect answer. Maximum failed attempts reached (5/5). Account recovery temporarily locked. Please try again later."
                                } else {
                                    dialogErrorMessage = "Incorrect answer. Please try again."
                                }
                            }
                        },
                        enabled = !isAttemptsBlocked && answerInput.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        modifier = Modifier.testTag("forgot_verify_answer_button")
                    ) {
                        Text("Verify Answer")
                    }
                }
                3 -> {
                    Button(
                        onClick = {
                            if (!isPasswordValid) {
                                dialogErrorMessage = "Please ensure password meets all requirements."
                                return@Button
                            }
                            if (!isPasswordMatch) {
                                dialogErrorMessage = "Passwords do not match."
                                return@Button
                            }

                            val user = foundUser ?: return@Button
                            isResetLoading = true
                            dialogErrorMessage = null
                            scope.launch {
                                val result = authRepository.resetPassword(user.email, newPassword)
                                isResetLoading = false
                                if (result.isSuccess) {
                                    currentStep = 4
                                } else {
                                    dialogErrorMessage = result.exceptionOrNull()?.message ?: "Failed to reset password."
                                }
                            }
                        },
                        enabled = !isResetLoading && isPasswordValid && isPasswordMatch,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        modifier = Modifier.testTag("forgot_change_password_button")
                    ) {
                        if (isResetLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Change Password")
                        }
                    }
                }
                4 -> {
                    Button(
                        onClick = {
                            val email = foundUser?.email ?: identifierInput
                            onPasswordResetSuccess(email)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        modifier = Modifier.testTag("back_to_login_button")
                    ) {
                        Text("Back to Sign In")
                    }
                }
            }
        },
        dismissButton = {
            if (currentStep < 4) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun PasswordRequirementItem(
    label: String,
    isMet: Boolean,
    isTouched: Boolean
) {
    val iconColor = if (isMet) {
        SuccessGreen
    } else if (isTouched) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    val textColor = if (isMet) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else if (isTouched) Icons.Default.Cancel else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = textColor,
                fontWeight = if (isMet) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}
