package com.example.arts

import com.example.arts.utils.TotpHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests the two-factor authentication codes. */
class TotpHelperTest {

    @Test
    fun aCodeGeneratedNowIsAccepted() {
        val secret = TotpHelper.generateSecret()
        assertTrue(TotpHelper.verifyCode(secret, TotpHelper.currentCode(secret)))
    }

    @Test
    fun aWrongCodeIsRejected() {
        val secret = TotpHelper.generateSecret()
        val current = TotpHelper.currentCode(secret)
        val wrong = if (current == "123456") "654321" else "123456"
        assertFalse(TotpHelper.verifyCode(secret, wrong))
    }

    @Test
    fun malformedCodesAreRejectedWithoutCrashing() {
        val secret = TotpHelper.generateSecret()
        assertFalse(TotpHelper.verifyCode(secret, "abcdef"))
        assertFalse(TotpHelper.verifyCode(secret, "12345"))
        assertFalse(TotpHelper.verifyCode(secret, ""))
    }

    @Test
    fun theSetupLinkNamesTheAppAndAccount() {
        val uri = TotpHelper.buildOtpAuthUri("ABCDEF", "me@arts.co.za")
        assertTrue(uri.startsWith("otpauth://totp/"))
        assertTrue(uri.contains("secret=ABCDEF"))
        assertTrue(uri.contains("ARTS"))
    }
}
