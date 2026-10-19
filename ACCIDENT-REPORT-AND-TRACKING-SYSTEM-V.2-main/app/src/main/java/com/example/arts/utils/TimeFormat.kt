package com.example.arts.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Reads and writes the ISO-8601 UTC timestamps the website stores, and turns them into "5 min ago" text. */
object TimeFormat {

    private val PATTERNS = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")

    // SimpleDateFormat is not thread-safe, so a fresh one is created for each call.
    private fun formatter(pattern: String) = SimpleDateFormat(pattern, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }

    /** Milliseconds since the epoch, or null if the text is blank or not a recognised timestamp. */
    fun parse(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        for (pattern in PATTERNS) {
            try {
                return formatter(pattern).parse(iso)?.time
            } catch (e: Exception) {
                // try the next pattern
            }
        }
        return null
    }

    fun nowIso(): String = formatter(PATTERNS[0]).format(Date())

    fun ago(iso: String?, nowMillis: Long = System.currentTimeMillis()): String {
        val then = parse(iso) ?: return ""
        val minutes = (nowMillis - then) / 60_000
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 60 * 24 -> "${minutes / 60} hr ago"
            else -> {
                val days = minutes / (60 * 24)
                if (days == 1L) "1 day ago" else "$days days ago"
            }
        }
    }
}
