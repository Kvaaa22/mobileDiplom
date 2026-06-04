package com.example.geometka.maps

import android.content.Context
import com.example.geometka.api.ApiConfig
import java.util.UUID

object DeviceIdentity {

    private const val PREFS_NAME = "device_identity"
    private const val KEY_DEVICE_ID = "device_id"

    fun getDeviceId(context: Context): String {
        if (ApiConfig.MOBILE_DEVICE_ID.isNotBlank()) {
            return ApiConfig.MOBILE_DEVICE_ID
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val existingId = prefs.getString(KEY_DEVICE_ID, null)
        if (!existingId.isNullOrBlank()) {
            return existingId
        }

        val newId = "mobile-${UUID.randomUUID()}"

        prefs.edit()
            .putString(KEY_DEVICE_ID, newId)
            .apply()

        return newId
    }
}
