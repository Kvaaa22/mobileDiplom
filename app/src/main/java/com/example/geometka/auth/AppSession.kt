package com.example.geometka.auth

import android.content.Context

object AppSession {

    private const val PREFS_NAME = "app_session"
    private const val KEY_IS_UNLOCKED = "is_unlocked"
    private const val KEY_USERNAME = "username"

    fun unlock(context: Context, username: String = "Демо-пользователь") {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_UNLOCKED, true)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun isUnlocked(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_UNLOCKED, false)
    }

    fun getUsername(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USERNAME, "Гость") ?: "Гость"
    }

    fun lock(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}