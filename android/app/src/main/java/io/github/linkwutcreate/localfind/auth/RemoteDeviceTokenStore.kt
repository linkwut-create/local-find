package io.github.linkwutcreate.localfind.auth

import android.content.Context
import android.content.SharedPreferences
import io.github.linkwutcreate.localfind.server.DiscoveredDevice
import org.json.JSONArray
import org.json.JSONObject

data class SavedDevice(
    val name: String,
    val host: String,
    val port: Int,
    val lastConnectedAt: Long
)

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

    fun clearRecentDevice() {
        prefs.edit()
            .remove("recent_name")
            .remove("recent_host")
            .remove("recent_port")
            .apply()
    }

    fun saveDevice(name: String, host: String, port: Int) {
        val devices = loadSavedDevices().toMutableList()
        val existing = devices.indexOfFirst { it.host == host && it.port == port }
        val entry = SavedDevice(name = name, host = host, port = port, lastConnectedAt = System.currentTimeMillis())
        if (existing >= 0) {
            devices[existing] = entry
        } else {
            devices.add(0, entry)
        }
        saveSavedDevices(devices)
    }

    fun getSavedDevices(): List<SavedDevice> {
        return loadSavedDevices()
    }

    fun removeSavedDevice(host: String, port: Int) {
        val devices = loadSavedDevices().filterNot { it.host == host && it.port == port }
        saveSavedDevices(devices)
    }

    private fun loadSavedDevices(): List<SavedDevice> {
        val raw = prefs.getString("saved_devices", "[]") ?: "[]"
        val array = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val host = obj.optString("host")
                if (host.isBlank()) continue
                add(
                    SavedDevice(
                        name = obj.optString("name", "Device"),
                        host = host,
                        port = obj.optInt("port", 8888),
                        lastConnectedAt = obj.optLong("lastConnectedAt", 0L)
                    )
                )
            }
        }
    }

    private fun saveSavedDevices(devices: List<SavedDevice>) {
        val array = JSONArray()
        devices.forEach { device ->
            array.put(
                JSONObject()
                    .put("name", device.name)
                    .put("host", device.host)
                    .put("port", device.port)
                    .put("lastConnectedAt", device.lastConnectedAt)
            )
        }
        prefs.edit().putString("saved_devices", array.toString()).apply()
    }
}
