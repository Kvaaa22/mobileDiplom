package com.example.geometka.api

import android.content.Context
import com.example.geometka.auth.AppSession
import com.example.geometka.data.Mark
import com.example.geometka.maps.DeviceIdentity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MarkSyncClient(
    private val context: Context
) {

    fun sendMarks(marks: List<Mark>) {
        if (marks.isEmpty()) return

        val deviceId = DeviceIdentity.getDeviceId(context)
        val errors = mutableListOf<String>()

        SYNC_ENDPOINTS.forEach { path ->
            val url = "${ApiConfig.BASE_URL}$path"
            try {
                postJson(url, buildSyncPackage(deviceId, marks).toString(), deviceId)
                return
            } catch (error: Exception) {
                errors.add("${path}: ${error.message ?: error.javaClass.simpleName}")
            }
        }

        throw IllegalStateException(errors.joinToString(separator = "; "))
    }

    fun sendMark(mark: Mark) {
        sendMarks(listOf(mark))
    }

    private fun buildSyncPackage(
        deviceId: String,
        marks: List<Mark>
    ): JSONObject {
        return JSONObject()
            .put("deviceId", deviceId)
            .put("clientTimestamp", System.currentTimeMillis())
            .put("marks", JSONArray().apply {
                marks.forEach { put(buildMarkBody(it)) }
            })
    }

    private fun buildMarkBody(mark: Mark): JSONObject {
        return JSONObject()
            .put("localId", mark.id)
            .put("name", mark.name)
            .put("latitude", mark.latitude)
            .put("longitude", mark.longitude)
            .put("pointType", mark.pointType.name)
            .put("intensity", mark.intensity.name)
            .put("typeOfFire", mark.typeOfFire.name)
            .put("notes", mark.notes)
            .put("createdAt", mark.createdAt)
            .put("horizontalAccuracyMeters", mark.horizontalAccuracyMeters)
            .put("mapId", mark.mapId)
    }

    private fun postJson(
        urlText: String,
        body: String,
        deviceId: String
    ) {
        val connection = URL(urlText).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-Device-Id", deviceId)
            connection.setRequestProperty("X-App-Client-Token", APP_CLIENT_TOKEN)
            setAuthHeader(connection)

            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                connection.inputStream?.close()
                return
            }

            val responseText = connection.errorStream
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            throw IllegalStateException(readErrorMessage(urlText, responseText, responseCode))
        } finally {
            connection.disconnect()
        }
    }

    private fun setAuthHeader(connection: HttpURLConnection) {
        val accessToken = AppSession.getAccessToken(context) ?: return
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
    }

    private fun readErrorMessage(
        urlText: String,
        responseText: String,
        responseCode: Int
    ): String {
        val path = URL(urlText).path
        if (responseText.isBlank()) {
            return "HTTP $responseCode on $path"
        }

        val message = runCatching {
            val obj = JSONObject(responseText)
            obj.optString("message")
                .ifBlank { obj.optString("error") }
                .ifBlank { responseText.take(MAX_ERROR_BODY_LENGTH) }
        }.getOrDefault(responseText.take(MAX_ERROR_BODY_LENGTH))

        return "HTTP $responseCode on $path: $message"
    }

    private companion object {
        const val APP_CLIENT_TOKEN = "demo-map-client-token"
        const val MAX_ERROR_BODY_LENGTH = 180

        val SYNC_ENDPOINTS = listOf(
            "/api/mobile/marks/sync",
            "/api/mobile/sync"
        )
    }
}
