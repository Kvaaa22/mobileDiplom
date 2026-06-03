package com.example.geometka.maps

import android.content.Context
import com.example.geometka.api.ApiConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MapDownloadDiagnostics {

    private const val PREFS_NAME = "map_download_diagnostics"
    private const val KEY_STAGE = "stage"
    private const val KEY_DETAIL = "detail"
    private const val KEY_UPDATED_AT = "updated_at"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_URL = "url"
    private const val KEY_PROGRESS_PERCENT = "progress_percent"
    private const val KEY_DOWNLOADED_BYTES = "downloaded_bytes"
    private const val KEY_TOTAL_BYTES = "total_bytes"

    data class Snapshot(
        val stage: String,
        val detail: String,
        val updatedAt: Long,
        val deviceId: String,
        val url: String,
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    )

    fun record(
        context: Context,
        stage: String,
        detail: String = "",
        url: String = "${ApiConfig.BASE_URL}/api/mobile/map-assignment",
        progressPercent: Int = -1,
        downloadedBytes: Long = 0L,
        totalBytes: Long = 0L
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STAGE, stage)
            .putString(KEY_DETAIL, detail)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .putString(KEY_DEVICE_ID, DeviceIdentity.getDeviceId(context))
            .putString(KEY_URL, url)
            .putInt(KEY_PROGRESS_PERCENT, progressPercent)
            .putLong(KEY_DOWNLOADED_BYTES, downloadedBytes)
            .putLong(KEY_TOTAL_BYTES, totalBytes)
            .apply()
    }

    fun snapshot(context: Context): Snapshot {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        return Snapshot(
            stage = prefs.getString(KEY_STAGE, "Загрузка карты еще не запускалась") ?: "",
            detail = prefs.getString(KEY_DETAIL, "") ?: "",
            updatedAt = prefs.getLong(KEY_UPDATED_AT, 0L),
            deviceId = prefs.getString(KEY_DEVICE_ID, DeviceIdentity.getDeviceId(context)) ?: "",
            url = prefs.getString(KEY_URL, "${ApiConfig.BASE_URL}/api/mobile/map-assignment") ?: "",
            progressPercent = prefs.getInt(KEY_PROGRESS_PERCENT, -1),
            downloadedBytes = prefs.getLong(KEY_DOWNLOADED_BYTES, 0L),
            totalBytes = prefs.getLong(KEY_TOTAL_BYTES, 0L)
        )
    }

    fun formatUpdatedAt(updatedAt: Long): String {
        if (updatedAt <= 0L) return "нет данных"

        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(updatedAt))
    }
}
