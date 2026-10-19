package com.example.arts.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/** Turns Firebase exceptions into messages a person can act on. */
object AuthErrors {

    fun friendly(e: Exception): String = when (e) {
        is FirebaseAuthUserCollisionException -> "An account with this email already exists"
        is FirebaseAuthWeakPasswordException -> "Use a password of at least 8 characters"
        is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password"
        is FirebaseTooManyRequestsException -> "Too many attempts. Wait a few minutes and try again"
        is FirebaseNetworkException -> "No connection. Check your internet and try again"
        else -> e.message ?: "Something went wrong. Please try again"
    }
}
