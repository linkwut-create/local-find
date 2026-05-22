package com.example.localfind.auth

import android.content.Context
import android.content.SharedPreferences

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
}
