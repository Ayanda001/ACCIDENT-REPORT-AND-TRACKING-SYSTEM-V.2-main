package com.example.arts.utils

/**
 * Remembers, for the life of the app process, which account has already
 * entered its two-factor code. Cleared on sign-out and whenever the app
 * is closed and reopened, so a code is asked for again in each new session.
 */
object SessionGate {
    @Volatile
    var totpVerifiedUid: String? = null

    fun clear() {
        totpVerifiedUid = null
    }
}
