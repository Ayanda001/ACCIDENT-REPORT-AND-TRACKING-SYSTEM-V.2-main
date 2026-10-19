package com.example.arts

import com.example.arts.utils.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests the input checks that stop invalid sign-in and registration details reaching Firebase. */
class ValidatorsTest {

    @Test
    fun acceptsAValidEmail() = assertTrue(Validators.isValidEmail("test@arts.co.za"))

    @Test
    fun rejectsAnInvalidEmail() {
        assertFalse(Validators.isValidEmail("not-an-email"))
        assertFalse(Validators.isValidEmail("missing@domain"))
        assertFalse(Validators.isValidEmail(""))
    }

    @Test
    fun passwordNeedsEightCharacters() {
        assertFalse(Validators.isStrongPassword("short"))
        assertTrue(Validators.isStrongPassword("Test1234"))
    }

    @Test
    fun phoneAcceptsDigitsSpacesAndPlus() {
        assertTrue(Validators.isValidPhone("082 123 4567"))
        assertTrue(Validators.isValidPhone("+27 82 123 4567"))
    }

    @Test
    fun phoneRejectsLettersAndShortNumbers() {
        assertFalse(Validators.isValidPhone("abc"))
        assertFalse(Validators.isValidPhone("12345"))
    }

    @Test
    fun registrationReportsBlankFieldsFirst() {
        assertEquals("Please fill in all fields", Validators.registrationError("", "a@b.co", "0821234567", "Test1234"))
    }

    @Test
    fun registrationReportsABadEmail() {
        assertEquals("Enter a valid email address", Validators.registrationError("Sam", "bad", "0821234567", "Test1234"))
    }

    @Test
    fun registrationReportsAWeakPassword() {
        assertEquals("Use a password of at least 8 characters", Validators.registrationError("Sam", "a@b.co", "0821234567", "abc"))
    }

    @Test
    fun registrationAcceptsGoodDetails() {
        assertNull(Validators.registrationError("Sam", "a@b.co", "0821234567", "Test1234"))
    }

    @Test
    fun loginReportsBlankAndBadInput() {
        assertEquals("Please fill in all fields", Validators.loginError("a@b.co", ""))
        assertEquals("Enter a valid email address", Validators.loginError("nope", "Test1234"))
        assertNull(Validators.loginError("a@b.co", "Test1234"))
    }
}
