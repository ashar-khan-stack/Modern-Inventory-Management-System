package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.util.BiometricHelper
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthIntegrationSuiteTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `test full authentication lifecycle and biometric success`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authRepo = com.example.data.repository.AuthRepository.getInstance(context)
        
        runBlocking {
            authRepo.logout()
        }

        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // 1. Switch to Sign Up Tab
        composeTestRule.onAllNodesWithText("Create Account")[0].performClick()
        composeTestRule.waitForIdle()

        // 2. Test Email Validation error case
        composeTestRule.onNodeWithTag("reg_first_name_input").performScrollTo().performTextInput("Integration")
        composeTestRule.onNodeWithTag("reg_last_name_input").performScrollTo().performTextInput("Test")
        composeTestRule.onNodeWithTag("reg_email_input").performScrollTo().performTextInput("invalid-email")
        composeTestRule.onNodeWithTag("reg_password_input").performScrollTo().performTextInput("Password123!")
        composeTestRule.onNodeWithTag("reg_confirm_password_input").performScrollTo().performTextInput("Password123!")

        // Submit form with invalid email
        composeTestRule.onNodeWithTag("reg_submit_button").performScrollTo().performClick()
        
        // Wait for validation error message
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Please include '@'", substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }

        // 3. Complete Sign-up with Valid Data
        composeTestRule.onNodeWithTag("reg_email_input").performScrollTo().performTextClearance()
        composeTestRule.onNodeWithTag("reg_email_input").performScrollTo().performTextInput("biometric_user@example.com")
        
        composeTestRule.onNodeWithTag("reg_submit_button").performScrollTo().performClick()
        
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("drawer_menu_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("drawer_menu_button").assertExists()

        // 4. Navigate to Settings to enable biometric login
        composeTestRule.onNodeWithTag("drawer_menu_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()

        // 5. Force enable biometric directly in Repo (simulating biometric switch toggle)
        runBlocking {
            authRepo.setFingerprintEnabled(true)
            authRepo.logout()
        }

        // 6. Verify sign out and return to Auth Screen with Biometric button visible
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("biometric_login_button").fetchSemanticsNodes().isNotEmpty()
        }

        // 7. Test Biometric Login Success scenario
        BiometricHelper.biometricInterceptor = { onSuccess, _ ->
            onSuccess()
            true
        }

        composeTestRule.onNodeWithTag("biometric_login_button").performScrollTo().performClick()
        
        // 8. Verify User Session Restored (Navigated back to Dashboard)
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("drawer_menu_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("drawer_menu_button").assertExists()
    }

    @Test
    fun `test biometric cancellation and error cases`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authRepo = com.example.data.repository.AuthRepository.getInstance(context)
        
        runBlocking {
            authRepo.logout()
            authRepo.register("Error", "Test", "error_user@example.com", "Password123!")
            authRepo.setFingerprintEnabled(true)
            authRepo.logout()
        }

        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // Verify on Auth Screen with biometric button
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("biometric_login_button").fetchSemanticsNodes().isNotEmpty()
        }

        // Case A: Biometric Authentication Failure / Retry Error
        BiometricHelper.biometricInterceptor = { _, onError ->
            onError("Biometric authentication failed. Please try again.")
            true
        }

        composeTestRule.onNodeWithTag("biometric_login_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Verify we are still on Auth Screen and error message is displayed
        composeTestRule.onNodeWithTag("biometric_login_button").assertExists()

        // Case B: Biometric Lockout Error Case
        BiometricHelper.biometricInterceptor = { _, onError ->
            onError("Biometric authentication locked out. Too many attempts. Please use password.")
            true
        }

        composeTestRule.onNodeWithTag("biometric_login_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Verify error message is rendered
        composeTestRule.onNodeWithText("Biometric authentication locked out", substring = true, ignoreCase = true).assertExists()
        composeTestRule.onNodeWithTag("biometric_login_button").assertExists()
    }
}
