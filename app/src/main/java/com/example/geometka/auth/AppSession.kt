package com.example.geometka.auth

import android.content.Context

object AppSession {

    private const val PREFS_NAME = "app_session"
    private const val KEY_IS_UNLOCKED = "is_unlocked"
    private const val KEY_USERNAME = "username"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_ID = "user_id"

    fun unlock(
        context: Context,
        username: String = "Demo user",
        accessToken: String? = null,
        refreshToken: String? = null,
        userId: String? = null
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_UNLOCKED, true)
            .putString(KEY_USERNAME, username)
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun isUnlocked(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_UNLOCKED, false)
    }

    fun getUsername(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USERNAME, "Guest") ?: "Guest"
    }

    fun getAccessToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACCESS_TOKEN, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun getRefreshToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_REFRESH_TOKEN, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun getUserId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_ID, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun lock(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
