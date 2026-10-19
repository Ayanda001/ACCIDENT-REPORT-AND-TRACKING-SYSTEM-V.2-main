package com.example.arts.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Small on-device preferences for user settings and local state.
 */
object PrefsHelper {

    private const val PREFS_NAME = "arts_prefs"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_PHONE = "user_phone"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_BIOMETRIC_UID = "biometric_uid"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_CUSTOM_AI_KEY = "custom_ai_key"
    private const val KEY_CUSTOM_AI_MODEL = "custom_ai_model"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveUser(context: Context, name: String, email: String, phone: String) {
        getPrefs(context).edit()
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_PHONE, phone)
            .apply()
    }

    fun getUserName(context: Context): String =
        getPrefs(context).getString(KEY_USER_NAME, "Driver") ?: "Driver"

    fun getUserEmail(context: Context): String =
        getPrefs(context).getString(KEY_USER_EMAIL, "") ?: ""

    fun getUserPhone(context: Context): String =
        getPrefs(context).getString(KEY_USER_PHONE, "") ?: ""

    fun logout(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PHONE)
            .apply()
    }

    fun setCustomAiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_CUSTOM_AI_KEY, key).apply()
    }

    fun getCustomAiKey(context: Context): String =
        getPrefs(context).getString(KEY_CUSTOM_AI_KEY, "") ?: ""

    fun setCustomAiModel(context: Context, model: String) {
        getPrefs(context).edit().putString(KEY_CUSTOM_AI_MODEL, model).apply()
    }

    fun getCustomAiModel(context: Context): String =
        getPrefs(context).getString(KEY_CUSTOM_AI_MODEL, "gemini-1.5-flash") ?: "gemini-1.5-flash"

    // --- Biometric unlock ---
    fun enableBiometric(context: Context, uid: String) {
        getPrefs(context).edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, true)
            .putString(KEY_BIOMETRIC_UID, uid)
            .apply()
    }

    fun disableBiometric(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .remove(KEY_BIOMETRIC_UID)
            .apply()
    }

    fun isBiometricEnabledFor(context: Context, uid: String?): Boolean =
        uid != null &&
            getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false) &&
            getPrefs(context).getString(KEY_BIOMETRIC_UID, null) == uid

    // --- Appearance ---
    fun getThemeMode(context: Context): String =
        getPrefs(context).getString(KEY_THEME_MODE, "light") ?: "light"

    fun setThemeMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_THEME_MODE, mode).apply()
    }
}
