package com.example.arts.utils

import dev.turingcomplete.kotlinonetimepassword.GoogleAuthenticator
import java.security.SecureRandom
import java.util.Date

/**
 * Wraps dev.turingcomplete:kotlin-onetimepassword's GoogleAuthenticator
 * decorator to give the app simple functions for TOTP-based 2FA
 * (compatible with Google Authenticator, Authy, etc - 6 digits, SHA1,
 * 30s step, which is what GoogleAuthenticator hard-codes for us).
 *
 * Secrets are always handled as Base32 strings, since that's what
 * GoogleAuthenticator expects and what authenticator apps expect too.
 */
object TotpHelper {

    private const val STEP_MILLIS = 30_000L

    /** Generates a new random 20-byte secret, Base32-encoded for authenticator apps. */
    fun generateSecret(): String {
        val bytes = ByteArray(20)
        SecureRandom().nextBytes(bytes)
        return base32Encode(bytes)
    }

    /** Builds the otpauth:// URI an authenticator app (or a future QR code) can scan/import. */
    fun buildOtpAuthUri(secret: String, accountEmail: String): String {
        val issuer = "ARTS"
        val label = "$issuer:$accountEmail"
        return "otpauth://totp/$label?secret=$secret&issuer=$issuer&digits=6&period=30"
    }

    /** Returns the currently valid 6-digit code for the given secret (handy for manual testing). */
    fun currentCode(secret: String): String {
        return GoogleAuthenticator(secret).generate(Date())
    }

    /**
     * Verifies a user-entered code, allowing one 30s step of clock drift
     * in either direction (standard practice for TOTP UX, since the
     * user's phone clock and the device clock can be slightly out of sync).
     */
    fun verifyCode(secret: String, code: String): Boolean {
        if (code.length != 6 || code.any { !it.isDigit() }) return false
        val now = System.currentTimeMillis()
        val authenticator = GoogleAuthenticator(secret)
        return (-1..1).any { drift ->
            authenticator.isValid(code, Date(now + drift * STEP_MILLIS))
        }
    }

    // --- Minimal RFC 4648 Base32 codec (no external dependency needed) ---

    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    private fun base32Encode(data: ByteArray): String {
        val output = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                bitsLeft -= 5
                output.append(BASE32_ALPHABET[(buffer shr bitsLeft) and 0x1F])
            }
        }
        if (bitsLeft > 0) {
            output.append(BASE32_ALPHABET[(buffer shl (5 - bitsLeft)) and 0x1F])
        }
        return output.toString()
    }
}
