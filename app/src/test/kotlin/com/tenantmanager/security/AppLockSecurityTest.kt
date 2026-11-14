package com.tenantmanager.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.tenantmanager.domain.repository.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLockSecurityTest {

    @Test
    fun defaultAppSettings_appLockIsDisabledByDefault() {
        val settings = AppSettings()
        assertFalse("App lock must be disabled by default", settings.appLockEnabled)
    }

    @Test
    fun biometricPrompt_configuresBiometricStrongAndDeviceCredentialFallback() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Collect Your Rent")
            .setSubtitle("Confirm your identity to access tenant records")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        assertNotNull(promptInfo)
        assertEquals("Unlock Collect Your Rent", promptInfo.title)
        val expectedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        assertEquals(expectedAuthenticators, promptInfo.allowedAuthenticators)
        // Verify that device-credential flag is present in the bitmask
        assertTrue(
            "DEVICE_CREDENTIAL bit must be enabled for PIN/password fallback",
            (promptInfo.allowedAuthenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL) != 0
        )
        // Verify that biometric strong flag is present in the bitmask
        assertTrue(
            "BIOMETRIC_STRONG bit must be enabled for fingerprint/face authentication",
            (promptInfo.allowedAuthenticators and BiometricManager.Authenticators.BIOMETRIC_STRONG) != 0
        )
    }
}

