package com.example.arts.utils

import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment

fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

/**
 * Safe toast that only shows if the fragment is currently attached to an activity.
 * Prevents "Fragment not attached to a context" crashes.
 */
fun Fragment.toast(message: String?) {
    val msg = message ?: return
    if (isAdded && context != null) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}

fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isValidPhone(): Boolean {
    return this.length >= 10 && this.all { it.isDigit() || it == '+' || it == '-' }
}
