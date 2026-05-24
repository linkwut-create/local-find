package io.github.linkwutcreate.localfind.store

import android.content.Context
import android.util.Base64
import io.github.linkwutcreate.localfind.model.PairedControllerToken
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom

class PairedControllerTokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("paired_controller_tokens", Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    @Synchronized
    fun issueToken(controllerId: String, controllerName: String, controllerType: String): String {
        val token = generateControlToken()
        val existing = loadAll().filterNot { it.controllerId == controllerId }
        val updated = existing + PairedControllerToken(
            controllerId = controllerId,
            controllerName = controllerName,
            controllerType = controllerType,
            token = token,
            pairedAt = System.currentTimeMillis(),
        )
        saveAll(updated)
        return token
    }

    @Synchronized
    fun isValidToken(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        return loadAll().any { it.token == token }
    }

    @Synchronized
    fun getAll(): List<PairedControllerToken> = loadAll()

    @Synchronized
    fun revokeByControllerId(controllerId: String): Boolean {
        if (controllerId.isBlank()) return false
        val existing = loadAll()
        val updated = existing.filterNot { it.controllerId == controllerId }
        if (updated.size == existing.size) return false
        saveAll(updated)
        return true
    }

    private fun generateControlToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun loadAll(): List<PairedControllerToken> {
        val raw = prefs.getString(KEY_CONTROLLERS, "[]") ?: "[]"
        val array = try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val controllerId = item.optString("controllerId")
                val token = item.optString("token")
                if (controllerId.isBlank() || token.isBlank()) continue
                add(
                    PairedControllerToken(
                        controllerId = controllerId,
                        controllerName = item.optString("controllerName", "Chrome Extension"),
                        controllerType = item.optString("controllerType", "chrome_extension"),
                        token = token,
                        pairedAt = item.optLong("pairedAt", 0L),
                    )
                )
            }
        }
    }

    private fun saveAll(tokens: List<PairedControllerToken>) {
        val array = JSONArray()
        tokens.forEach { token ->
            array.put(
                JSONObject()
                    .put("controllerId", token.controllerId)
                    .put("controllerName", token.controllerName)
                    .put("controllerType", token.controllerType)
                    .put("token", token.token)
                    .put("pairedAt", token.pairedAt)
            )
        }

        // MVP-L.1 stores controller tokens in plaintext because Android must validate incoming
        // command tokens. Harden this by storing token hashes before broader distribution.
        prefs.edit().putString(KEY_CONTROLLERS, array.toString()).apply()
    }

    companion object {
        private const val KEY_CONTROLLERS = "controllers"
    }
}
