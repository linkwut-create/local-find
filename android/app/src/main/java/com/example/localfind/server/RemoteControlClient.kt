package com.example.localfind.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed class ControlResult {
    data class Success(val statusJson: JSONObject? = null) : ControlResult()
    object Unauthorized : ControlResult()
    data class Error(val message: String) : ControlResult()
}

class RemoteControlClient {

    suspend fun getStatus(host: String, port: Int): ControlResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$host:$port/status")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                ControlResult.Success(JSONObject(responseText))
            } else {
                ControlResult.Error("HTTP Error: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            ControlResult.Error(e.message ?: "Unknown connection error")
        }
    }

    suspend fun sendCommand(host: String, port: Int, token: String, endpoint: String): ControlResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$host:$port$endpoint")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.setRequestProperty("X-LocalFind-Token", token)

            when (connection.responseCode) {
                200 -> ControlResult.Success()
                401 -> ControlResult.Unauthorized
                else -> ControlResult.Error("HTTP Error: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            ControlResult.Error(e.message ?: "Unknown connection error")
        }
    }
}
