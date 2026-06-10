package com.example.geometka.api

import android.content.Context
import com.example.geometka.data.Mark
import com.example.geometka.data.VerificationStatus
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
        val url = "${ApiConfig.BASE_URL}${ApiContract.MARK_SYNC_PATH}"
        val response = postJson(url, buildSyncPackage(marks).toString())
        return SyncResponseParser.parse(response).map { result ->
            MarkSyncResult(
                localId = result.localId,
                verificationStatus = result.verificationStatus
            )
        }
    }

    fun sendMark(mark: Mark) {
        sendMarks(listOf(mark))
    }

    private fun buildSyncPackage(marks: List<Mark>): JSONObject {
        return ApiContract.syncBody(
            clientTimestamp = System.currentTimeMillis(),
            marks = marks,
            markBody = ::buildMarkBody
        )
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
        body: String
    ): String {
        val connection = URL(urlText).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            AuthenticatedConnection.configure(context, connection)

            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            ApiSessionHandler.handleResponse(context, responseCode)

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
        const val MAX_ERROR_BODY_LENGTH = 180
    }
}
