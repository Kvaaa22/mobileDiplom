package com.example.geometka.api

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AuthApiClient {

    data class LoginResult(
        val accessToken: String,
        val username: String?
    )

    fun login(
        login: String,
        password: String
    ): LoginResult {
        val endpoint = URL("${ApiConfig.BASE_URL}${ApiContract.LOGIN_PATH}")
        return executeLogin(login, password, endpoint, 0)
    }

    private fun executeLogin(
        login: String,
        password: String,
        url: URL,
        redirectCount: Int
    ): LoginResult {
        if (redirectCount > MAX_REDIRECTS) {
            throw IllegalStateException("Too many server redirects")
        }

        val connection = url.openConnection() as HttpURLConnection

        return try {
            val requestBody = ApiContract.loginBody(login, password).toString()

            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")

            connection.outputStream.use { output ->
                output.write(requestBody.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode

            if (responseCode in REDIRECT_CODES) {
                val location = connection.getHeaderField("Location")
                    ?: throw IllegalStateException("Server returned redirect $responseCode without Location")
                val redirectedUrl = URL(url, location)
                return executeLogin(login, password, redirectedUrl, redirectCount + 1)
            }

            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }

            if (responseCode !in 200..299) {
                if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    throw IllegalStateException("Пользователь заблокирован")
                }
                throw IllegalStateException(readErrorMessage(responseText, responseCode))
            }

            parseLoginResult(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseLoginResult(json: String): LoginResult {
        val obj = JSONObject(json)
        val accessToken = obj.optString("accessToken")
            .ifBlank { obj.optString("token") }
            .ifBlank { obj.optString("access_token") }

        if (accessToken.isBlank()) {
            throw IllegalStateException("Server response does not contain an access token")
        }

        return LoginResult(
            accessToken = accessToken,
            username = obj.optString("userName")
                .ifBlank { obj.optString("username") }
                .ifBlank { obj.optString("name") }
                .ifBlank { obj.optString("login") }
                .ifBlank { null }
        )
    }

    private fun readErrorMessage(
        responseText: String,
        responseCode: Int
    ): String {
        if (responseText.isBlank()) {
            return "Server returned code $responseCode"
        }

        return runCatching {
            val obj = JSONObject(responseText)
            obj.optString("message")
                .ifBlank { obj.optString("error") }
                .ifBlank { "Server returned code $responseCode" }
        }.getOrDefault("Server returned code $responseCode")
    }

    private companion object {
        const val MAX_REDIRECTS = 5
        val REDIRECT_CODES = setOf(
            HttpURLConnection.HTTP_MOVED_PERM,
            HttpURLConnection.HTTP_MOVED_TEMP,
            HttpURLConnection.HTTP_SEE_OTHER,
            307,
            308
        )
    }
}
