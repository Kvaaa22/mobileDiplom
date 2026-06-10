package com.example.geometka.maps

import android.content.Context
import com.example.geometka.api.ApiConfig
import com.example.geometka.api.ApiSessionHandler
import com.example.geometka.api.AuthenticatedConnection
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class MapAssignmentClient(
    private val context: Context
) {

    fun fetchAssignedPackage(): MapPackage? {
        val urlText = "${ApiConfig.BASE_URL}${com.example.geometka.api.ApiContract.MAP_ASSIGNMENT_PATH}"
        val connection = URL(urlText).openConnection() as HttpURLConnection

        return try {
            MapDownloadDiagnostics.record(
                context = context,
                stage = "Requesting map assignment",
                detail = "Authorized user",
                url = urlText
            )

            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            AuthenticatedConnection.configure(context, connection)

            val responseCode = connection.responseCode
            ApiSessionHandler.handleResponse(context, responseCode)

            when (responseCode) {
                HttpURLConnection.HTTP_NO_CONTENT -> {
                    MapDownloadDiagnostics.record(
                        context = context,
                        stage = "No map assignment",
                        detail = "HTTP 204 for authorized user",
                        url = urlText
                    )
                    null
                }

                HttpURLConnection.HTTP_NOT_FOUND -> {
                    MapDownloadDiagnostics.record(
                        context = context,
                        stage = "Map assignment not found",
                        detail = "HTTP 404 for authorized user",
                        url = urlText
                    )
                    null
                }

                HttpURLConnection.HTTP_OK -> {
                    val response = connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                    MapDownloadDiagnostics.record(
                        context = context,
                        stage = "Map assignment received",
                        detail = "HTTP 200, ${response.length} bytes",
                        url = urlText
                    )

                    parseMapPackage(response)
                }

                else -> {
                    val errorBody = connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                        .take(180)

                    throw IllegalStateException(
                        "Map assignment HTTP $responseCode: $errorBody"
                    )
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
        val connection = URL(mapPackage.downloadUrl).openConnection() as HttpURLConnection

        try {
            MapDownloadDiagnostics.record(
                context = context,
                stage = "Downloading map file",
                detail = "Package ${mapPackage.remoteId}, version ${mapPackage.version}",
                url = mapPackage.downloadUrl,
                progressPercent = 0
            )

            connection.requestMethod = "GET"
            connection.connectTimeout = 20_000
            connection.readTimeout = 60_000
            AuthenticatedConnection.configure(context, connection)

            val responseCode = connection.responseCode
            ApiSessionHandler.handleResponse(context, responseCode)

            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
                    .take(180)

                throw IllegalStateException(
                    "Map download HTTP $responseCode: $errorBody"
                )
            }

            if (tempFile.exists()) {
                tempFile.delete()
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0L }
                ?: mapPackage.fileSizeBytes
                ?: 0L
            var downloadedBytes = 0L
            var lastReportedPercent = -1

            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read = input.read(buffer)

                    while (read != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read.toLong()

                        val percent = if (totalBytes > 0L) {
                            ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            -1
                        }

                        if (percent != lastReportedPercent) {
                            lastReportedPercent = percent
                            MapDownloadDiagnostics.record(
                                context = context,
                                stage = "Downloading map file",
                                detail = "Downloaded $downloadedBytes of $totalBytes bytes",
                                url = mapPackage.downloadUrl,
                                progressPercent = percent,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes
                            )
                        }

                        read = input.read(buffer)
                    }

                    output.flush()
                }
            }

            MapDownloadDiagnostics.record(
                context = context,
                stage = "Map file downloaded",
                detail = "Temp file ${tempFile.length()} bytes",
                url = mapPackage.downloadUrl,
                progressPercent = 100,
                downloadedBytes = tempFile.length(),
                totalBytes = totalBytes
            )
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
            downloadUrl = normalizeLocalServerUrl(obj.getString("downloadUrl")),
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

    private fun normalizeLocalServerUrl(rawUrl: String): String {
        return try {
            val uri = URI(rawUrl)
            val host = uri.host ?: return rawUrl

            if (host != "localhost" && host != "127.0.0.1") {
                return rawUrl
            }

            val baseUri = URI(ApiConfig.BASE_URL)
            val replacementHost = baseUri.host ?: return rawUrl

            URI(
                uri.scheme,
                uri.userInfo,
                replacementHost,
                baseUri.port.takeIf { it != -1 } ?: uri.port,
                uri.path,
                uri.query,
                uri.fragment
            ).toString()
        } catch (_: Exception) {
            rawUrl
        }
    }

}
