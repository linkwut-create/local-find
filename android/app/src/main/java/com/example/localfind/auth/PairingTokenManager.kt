package com.example.localfind.auth

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class PairingTokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pairing_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "pairing_token"
    }

    init {
        if (getToken() == null) {
            regenerateToken()
        }
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun regenerateToken(): String {
        // 生成 8 位大写字母数字组合，方便手工输入
        val newToken = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        prefs.edit().putString(KEY_TOKEN, newToken).apply()
        return newToken
    }
}
