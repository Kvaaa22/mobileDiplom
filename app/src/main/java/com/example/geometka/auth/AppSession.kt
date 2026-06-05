package com.example.geometka.auth

import android.content.Context

object AppSession {

    private const val PREFS_NAME = "app_session"
    private const val KEY_IS_UNLOCKED = "is_unlocked"
    private const val KEY_USERNAME = "username"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_MAP_ASSIGNMENT_CHECKED_ACCOUNT = "map_assignment_checked_account"

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

    fun getAccountKey(context: Context): String? {
        if (!isUnlocked(context)) return null

        return getUserId(context)
            ?.let { "user:$it" }
            ?: getUsername(context)
                .takeIf { it.isNotBlank() && it != "Guest" }
                ?.let { "username:$it" }
    }

    fun markMapAssignmentChecked(context: Context, accountKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MAP_ASSIGNMENT_CHECKED_ACCOUNT, accountKey)
            .apply()
    }

    fun isMapAssignmentChecked(context: Context): Boolean {
        val accountKey = getAccountKey(context) ?: return false
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MAP_ASSIGNMENT_CHECKED_ACCOUNT, null) == accountKey
    }

    fun lock(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
