package com.example.geometka.maps

import android.content.Context
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MapAssignmentClient(
    private val context: Context
) {

    companion object {
        /*
         * Замени на адрес своего сервера.
         * Для ВКР можно оставить заглушку и потом подставить реальный API.
         */
        private const val BASE_URL = "https://example.com"

        /*
         * Прототипный ключ клиента.
         * В реальной системе лучше выдавать отдельный токен устройству после регистрации.
         */
        private const val APP_CLIENT_TOKEN = "demo-map-client-token"
    }

    fun fetchAssignedPackage(): MapPackage? {
        val deviceId = DeviceIdentity.getDeviceId(context)

        val url = URL("$BASE_URL/api/mobile/map-assignment?deviceId=$deviceId")
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("X-Device-Id", deviceId)
            connection.setRequestProperty("X-App-Client-Token", APP_CLIENT_TOKEN)

            when (connection.responseCode) {
                HttpURLConnection.HTTP_NO_CONTENT -> null
                HttpURLConnection.HTTP_NOT_FOUND -> null
                HttpURLConnection.HTTP_OK -> {
                    val response = connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                    parseMapPackage(response)
                }

                else -> {
                    throw IllegalStateException("Сервер вернул код ${connection.responseCode}")
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    fun downloadPackageToFile(
        mapPackage: MapPackage,
        tempFile: File
    ) {
        val url = URL(mapPackage.downloadUrl)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 20_000
            connection.readTimeout = 60_000
            connection.setRequestProperty("X-Device-Id", DeviceIdentity.getDeviceId(context))
            connection.setRequestProperty("X-App-Client-Token", APP_CLIENT_TOKEN)

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Ошибка загрузки карты: ${connection.responseCode}")
            }

            if (tempFile.exists()) {
                tempFile.delete()
            }

            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read = input.read(buffer)

                    while (read != -1) {
                        output.write(buffer, 0, read)
                        read = input.read(buffer)
                    }

                    output.flush()
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseMapPackage(json: String): MapPackage {
        val obj = JSONObject(json)

        return MapPackage(
            remoteId = obj.getLong("id"),
            name = obj.getString("name"),
            version = obj.getInt("version"),
            downloadUrl = obj.getString("downloadUrl"),
            checksumSha256 = obj.optString("checksumSha256").ifBlank { null },
            fileSizeBytes = if (obj.has("fileSizeBytes")) {
                obj.optLong("fileSizeBytes")
            } else {
                null
            },
            localPath = null,
            status = MapPackageStatus.ASSIGNED,
            downloadedAt = null,
            lastError = null
        )
    }
}