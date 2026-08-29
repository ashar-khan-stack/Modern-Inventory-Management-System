package com.example.ui.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    enum class BiometricStatus {
        AVAILABLE,
        NONE_ENROLLED,
        NO_HARDWARE,
        HARDWARE_UNAVAILABLE,
        UNSUPPORTED
    }

    fun checkBiometricStatus(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            else -> BiometricStatus.UNSUPPORTED
        }
    }

    fun isBiometricAvailable(context: Context): Boolean {
        return checkBiometricStatus(context) == BiometricStatus.AVAILABLE
    }

    // Testing hook to bypass system biometric prompt during integration tests
    var biometricInterceptor: ((onSuccess: () -> Unit, onError: (String) -> Unit) -> Boolean)? = null

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Biometric Verification",
        subtitle: String = "Authenticate to continue",
        negativeButtonText: String = "Use Password",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Check if test interceptor handles it
        if (biometricInterceptor?.invoke(onSuccess, onError) == true) {
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription("Confirm your biometric credentials to securely access your inventory and sales records.")
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // errorCode 13 is user cancel / negative button
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Biometric authentication failed. Please try again.")
                }
            }
        )

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            onError("Unable to launch biometric authentication: ${e.localizedMessage}")
        }
    }
}
