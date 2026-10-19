package com.example.arts.utils

/** Input checks shared by the sign-in and registration screens. Plain Kotlin, so they can be unit tested. */
object Validators {

    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean = EMAIL.matches(email.trim())

    fun isStrongPassword(password: String): Boolean = password.length >= 8

    fun isValidPhone(phone: String): Boolean {
        val digits = phone.count { it.isDigit() }
        return digits >= 9 && phone.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }
    }

    /** Returns an error message, or null if the registration details are acceptable. */
    fun registrationError(fullName: String, email: String, phone: String, password: String): String? = when {
        fullName.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank() -> "Please fill in all fields"
        !isValidEmail(email) -> "Enter a valid email address"
        !isValidPhone(phone) -> "Enter a valid phone number"
        !isStrongPassword(password) -> "Use a password of at least 8 characters"
        else -> null
    }

    /** Returns an error message, or null if the sign-in details are acceptable. */
    fun loginError(email: String, password: String): String? = when {
        email.isBlank() || password.isBlank() -> "Please fill in all fields"
        !isValidEmail(email) -> "Enter a valid email address"
        else -> null
    }
}
