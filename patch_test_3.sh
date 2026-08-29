cat << 'TESTEOF' > app/src/test/java/com/example/BiometricIntegrationTest.kt
package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.util.BiometricHelper
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BiometricIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `test full biometric flow`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authRepo = com.example.data.repository.AuthRepository.getInstance(context)
        kotlinx.coroutines.runBlocking {
            authRepo.register("Bio", "Test", "bio@test.com", "Password123!")
            authRepo.logout()
        }

        // We are on AuthScreen (Login)
        composeTestRule.onNodeWithTag("login_email_input").performTextInput("bio@test.com")
        composeTestRule.onNodeWithTag("login_password_input").performTextInput("Password123!")
        
        // Use performScrollTo in case keyboard hides it, then click
        composeTestRule.onNodeWithTag("login_submit_button").performScrollTo().performClick()

        // Wait for coroutine and navigation
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        
        // Wait for dashboard to load
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("drawer_menu_button").fetchSemanticsNodes().isNotEmpty()
        }

        // Open Drawer
        composeTestRule.onNodeWithTag("drawer_menu_button").performClick()
        composeTestRule.waitForIdle()

        // Click Settings
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()

        var biometricPromptShown = false
        BiometricHelper.biometricInterceptor = { onSuccess, _ ->
            biometricPromptShown = true
            onSuccess()
            true
        }

        // Click Biometric Switch to enable
        composeTestRule.onNodeWithTag("biometric_switch").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assert(biometricPromptShown) { "Biometric prompt should have been shown when enabling in Settings" }

        // Click Sign Out
        composeTestRule.onNodeWithTag("sign_out_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Sign Out").performClick()
        composeTestRule.waitForIdle()

        // Wait for auth screen
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("biometric_login_button").fetchSemanticsNodes().isNotEmpty()
        }

        // Test error scenario
        BiometricHelper.biometricInterceptor = { _, onError ->
            onError("Biometric authentication failed. Please try again.")
            true
        }

        composeTestRule.onNodeWithTag("biometric_login_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("biometric_login_button").assertExists()

        // Test success scenario
        BiometricHelper.biometricInterceptor = { onSuccess, _ ->
            onSuccess()
            true
        }

        composeTestRule.onNodeWithTag("biometric_login_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Verify we navigated to dashboard
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("drawer_menu_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("drawer_menu_button").assertExists()
    }
}
TESTEOF
