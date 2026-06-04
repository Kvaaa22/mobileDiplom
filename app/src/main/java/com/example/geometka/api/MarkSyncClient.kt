package com.example.geometka.api

import android.content.Context
import com.example.geometka.auth.AppSession
import com.example.geometka.data.Mark
import com.example.geometka.data.VerificationStatus
import com.example.geometka.maps.DeviceIdentity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MarkSyncClient(
    private val context: Context
) {

    data class MarkSyncResult(
        val localId: Long,
        val verificationStatus: VerificationStatus?
    )

    fun sendMarks(marks: List<Mark>): List<MarkSyncResult> {
        val deviceId = DeviceIdentity.getDeviceId(context)
        val errors = mutableListOf<String>()

        SYNC_ENDPOINTS.forEach { path ->
            val url = "${ApiConfig.BASE_URL}$path"
            try {
                val response = postJson(url, buildSyncPackage(deviceId, marks).toString(), deviceId)
                return parseSyncResults(response)
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
            .put("verificationStatus", mark.verificationStatus.name)
            .put("mapId", mark.mapId)
    }

    private fun postJson(
        urlText: String,
        body: String,
        deviceId: String
    ): String {
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
                return connection.inputStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
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

    private fun parseSyncResults(responseText: String): List<MarkSyncResult> {
        if (responseText.isBlank()) return emptyList()

        return runCatching {
            when (responseText.trim().firstOrNull()) {
                '[' -> parseResultArray(JSONArray(responseText))
                '{' -> parseResultObject(JSONObject(responseText))
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }

    private fun parseResultObject(obj: JSONObject): List<MarkSyncResult> {
        val directResult = parseResultItem(obj)
        if (directResult != null) return listOf(directResult)

        val resultKeys = listOf(
            "marks",
            "results",
            "accepted",
            "updates",
            "items",
            "records"
        )

        return resultKeys
            .asSequence()
            .mapNotNull { key -> obj.optJSONArray(key) }
            .flatMap { array -> parseResultArray(array).asSequence() }
            .toList()
    }

    private fun parseResultArray(array: JSONArray): List<MarkSyncResult> {
        val results = mutableListOf<MarkSyncResult>()

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            parseResultItem(item)?.let { results.add(it) }
        }

        return results
    }

    private fun parseResultItem(obj: JSONObject): MarkSyncResult? {
        val localId = optLongAny(
            obj,
            "localId",
            "clientId",
            "clientLocalId",
            "mobileId"
        ) ?: return null

        return MarkSyncResult(
            localId = localId,
            verificationStatus = parseVerificationStatus(
                optStringAny(
                    obj,
                    "verificationStatus",
                    "reviewStatus",
                    "status"
                )
            )
        )
    }

    private fun optLongAny(
        obj: JSONObject,
        vararg keys: String
    ): Long? {
        keys.forEach { key ->
            if (!obj.has(key) || obj.isNull(key)) return@forEach
            val value = obj.opt(key)

            when (value) {
                is Number -> return value.toLong()
                is String -> value.toLongOrNull()?.let { return it }
            }
        }

        return null
    }

    private fun optStringAny(
        obj: JSONObject,
        vararg keys: String
    ): String? {
        keys.forEach { key ->
            val value = obj.optString(key).takeIf { it.isNotBlank() }
            if (value != null) return value
        }

        return null
    }

    private fun parseVerificationStatus(value: String?): VerificationStatus? {
        return when (value?.trim()?.uppercase()) {
            "UNVERIFIED",
            "NOT_VERIFIED",
            "PENDING",
            "NEW",
            "NEEDS_REVIEW",
            "UNDER_REVIEW" -> VerificationStatus.UNVERIFIED

            "CONFIRMED",
            "APPROVED",
            "VERIFIED",
            "ACCEPTED" -> VerificationStatus.CONFIRMED

            "DISPROVED",
            "REJECTED",
            "DECLINED",
            "DENIED",
            "FALSE",
            "CANCELLED",
            "CANCELED" -> VerificationStatus.DISPROVED

            else -> null
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
