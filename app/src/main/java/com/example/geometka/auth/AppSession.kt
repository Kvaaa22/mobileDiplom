package com.example.geometka.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object AppSession {

    private const val PREFS_NAME = "app_session_encrypted"
    private const val KEY_IS_UNLOCKED = "is_unlocked"
    private const val KEY_USERNAME = "username"
    private const val KEY_ACCOUNT_NAME = "account_name"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_MAP_ASSIGNMENT_CHECKED_ACCOUNT = "map_assignment_checked_account"

    fun unlock(
        context: Context,
        username: String = "Demo user",
        accountName: String = username,
        accessToken: String? = "stub_token"
    ) {
        preferences(context)
            .edit()
            .putBoolean(KEY_IS_UNLOCKED, true)
            .putString(KEY_USERNAME, username)
            .putString(KEY_ACCOUNT_NAME, accountName)
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .remove(KEY_MAP_ASSIGNMENT_CHECKED_ACCOUNT)
            .apply()
    }

    fun isUnlocked(context: Context): Boolean {
        return preferences(context)
            .getBoolean(KEY_IS_UNLOCKED, false)
    }

    fun getUsername(context: Context): String {
        return preferences(context)
            .getString(KEY_USERNAME, "Guest") ?: "Guest"
    }

    fun getAccessToken(context: Context): String? {
        return preferences(context)
            .getString(KEY_ACCESS_TOKEN, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun getAccountKey(context: Context): String? {
        if (!isUnlocked(context)) return null

        return preferences(context)
            .getString(KEY_ACCOUNT_NAME, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { "username:$it" }
    }

    fun markMapAssignmentChecked(context: Context, accountKey: String) {
        preferences(context)
            .edit()
            .putString(KEY_MAP_ASSIGNMENT_CHECKED_ACCOUNT, accountKey)
            .apply()
    }

    fun isMapAssignmentChecked(context: Context): Boolean {
        val accountKey = getAccountKey(context) ?: return false
        return preferences(context)
            .getString(KEY_MAP_ASSIGNMENT_CHECKED_ACCOUNT, null) == accountKey
    }

    fun lock(context: Context) {
        preferences(context)
            .edit()
            .clear()
            .apply()
    }

    private fun preferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
