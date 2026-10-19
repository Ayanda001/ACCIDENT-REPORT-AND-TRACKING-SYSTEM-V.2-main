package com.example.arts.utils

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat

/**
 * Thin wrapper around androidx.biometric so fragments don't have to deal
 * with BiometricPrompt/CryptoObject boilerplate directly.
 *
 * This is used for a device-local "quick unlock" (fingerprint/face) that
 * re-authenticates a user who has already logged in with a password at
 * least once on this device. It is NOT a replacement for the
 * password/email login — it's a convenience layer stored per-device via
 * PrefsHelper.
 */
object BiometricHelper {

    enum class Availability {
        AVAILABLE,
        NO_HARDWARE,
        HARDWARE_UNAVAILABLE,
        NONE_ENROLLED,
        UNKNOWN
    }

    /**
     * Authenticators checked in order of preference:
     *   BIOMETRIC_STRONG  - dedicated fingerprint/iris chip (Pixel, most flagships)
     *   BIOMETRIC_WEAK    - face-unlock via front camera (many mid-range devices)
     *   DEVICE_CREDENTIAL - PIN / pattern / password (always available as a last resort)
     *
     * Returns AVAILABLE if ANY of these is set up, so the biometric button
     * appears on virtually every phone rather than only high-end ones.
     */
    fun checkAvailability(fragment: Fragment): Availability {
        val manager = BiometricManager.from(fragment.requireContext())
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return when (manager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Availability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Availability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NONE_ENROLLED
            else -> Availability.UNKNOWN
        }
    }

    /**
     * Shows the system biometric prompt.
     *
     * @param title shown at the top of the system prompt
     * @param subtitle shown under the title
     * @param onSuccess called when the user authenticates successfully
     * @param onError called with a human-readable message on failure/cancel
     */
    fun authenticate(
        fragment: Fragment,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(fragment.requireContext())

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Not fatal - user gets another attempt from the system UI.
            }
        }

        val prompt = BiometricPrompt(fragment, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        prompt.authenticate(promptInfo)
    }
}
