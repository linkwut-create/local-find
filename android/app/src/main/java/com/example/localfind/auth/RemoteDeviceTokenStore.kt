package com.example.localfind.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.localfind.server.DiscoveredDevice

class RemoteDeviceTokenStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("remote_tokens", Context.MODE_PRIVATE)

    fun getToken(host: String, port: Int): String? {
        return prefs.getString("$host:$port", null)
    }

    fun saveToken(host: String, port: Int, token: String) {
        prefs.edit().putString("$host:$port", token).apply()
    }

    fun clearToken(host: String, port: Int) {
        prefs.edit().remove("$host:$port").apply()
    }

    fun saveRecentDevice(name: String, host: String, port: Int) {
        prefs.edit()
            .putString("recent_name", name)
            .putString("recent_host", host)
            .putInt("recent_port", port)
            .apply()
    }

    fun getRecentDevice(): DiscoveredDevice? {
        val host = prefs.getString("recent_host", null) ?: return null
        val name = prefs.getString("recent_name", "Recent Device") ?: "Recent Device"
        val port = prefs.getInt("recent_port", 8888)
        return DiscoveredDevice(
            name = name,
            host = host,
            port = port,
            controlUrl = "http://$host:$port"
        )
    }
}
