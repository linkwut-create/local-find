package io.github.linkwutcreate.localfind.auth

import android.content.Context
import android.content.SharedPreferences
import io.github.linkwutcreate.localfind.server.DiscoveredDevice
import io.github.linkwutcreate.localfind.server.DiscoveryEndpointPolicy
import io.github.linkwutcreate.localfind.server.IdentityBindingPolicy
import org.json.JSONArray
import org.json.JSONObject

data class SavedDevice(
    val name: String,
    val host: String,
    val port: Int,
    val lastConnectedAt: Long,
    val deviceId: String = "",
)

class RemoteDeviceTokenStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("remote_tokens", Context.MODE_PRIVATE)

    /**
     * Legacy host:port tokens are intentionally not returned. A host/port
     * record is not identity proof and must not transfer trust automatically.
     */
    @Deprecated("Use getToken(DiscoveredDevice) after /device-info verification")
    fun getToken(host: String, port: Int): String? {
        return null
    }

    /** Keep old records available for explicit cleanup, but never use them as trust. */
    @Deprecated("Use saveToken(DiscoveredDevice, String)")
    fun saveToken(host: String, port: Int, token: String) {
        prefs.edit().putString("$host:$port", token).apply()
    }

    @Deprecated("Use clearToken(DiscoveredDevice)")
    fun clearToken(host: String, port: Int) {
        prefs.edit().remove("$host:$port").apply()
    }

    fun getToken(device: DiscoveredDevice): String? {
        val deviceId = verifiedDeviceId(device) ?: return null
        val saved = loadSavedDevices().firstOrNull { it.deviceId == deviceId } ?: return null
        if (!IdentityBindingPolicy.canTransferTrust(saved.deviceId, device.deviceId, device.identityVerified)) {
            return null
        }
        return prefs.getString(tokenKey(deviceId), null)
    }

    /** Persist a token only when the candidate endpoint carried verified identity. */
    fun saveToken(device: DiscoveredDevice, token: String): Boolean {
        val deviceId = verifiedDeviceId(device) ?: return false
        if (!saveDevice(device)) return false
        prefs.edit().putString(tokenKey(deviceId), token).apply()
        return true
    }

    fun clearToken(device: DiscoveredDevice) {
        IdentityBindingPolicy.normalizeDeviceId(device.deviceId)?.let { deviceId ->
            prefs.edit().remove(tokenKey(deviceId)).apply()
        }
    }

    fun clearTokenForDeviceId(deviceId: String) {
        IdentityBindingPolicy.normalizeDeviceId(deviceId)?.let { normalized ->
            prefs.edit().remove(tokenKey(normalized)).apply()
        }
    }

    fun saveRecentDevice(name: String, host: String, port: Int) {
        prefs.edit()
            .putString("recent_name", name)
            .putString("recent_host", host)
            .putInt("recent_port", port)
            .remove("recent_device_id")
            .apply()
    }

    fun saveRecentDevice(device: DiscoveredDevice) {
        prefs.edit()
            .putString("recent_name", device.name)
            .putString("recent_host", device.host)
            .putInt("recent_port", device.port)
            .putString("recent_device_id", IdentityBindingPolicy.normalizeDeviceId(device.deviceId) ?: "")
            .apply()
    }

    fun getRecentDevice(): DiscoveredDevice? {
        val host = prefs.getString("recent_host", null) ?: return null
        val name = prefs.getString("recent_name", "Recent Device") ?: "Recent Device"
        val port = prefs.getInt("recent_port", 8888)
        val deviceId = prefs.getString("recent_device_id", "") ?: ""
        return DiscoveredDevice(
            name = name,
            host = host,
            port = port,
            controlUrl = "http://$host:$port",
            deviceId = deviceId,
        )
    }

    fun clearRecentDevice() {
        prefs.edit()
            .remove("recent_name")
            .remove("recent_host")
            .remove("recent_port")
            .remove("recent_device_id")
            .apply()
    }

    /** Save a candidate; only a verified identity may update an existing paired endpoint. */
    fun saveDevice(device: DiscoveredDevice): Boolean {
        val devices = loadSavedDevices().toMutableList()
        val verifiedId = verifiedDeviceId(device)
        if (verifiedId == null && device.deviceId.isNotBlank()) {
            // A saved identity without fresh /device-info proof cannot create or
            // update an endpoint record, even when an explicit token succeeds.
            return false
        }
        val identityIndex = verifiedId?.let { id -> devices.indexOfFirst { it.deviceId == id } } ?: -1
        val addressIndex = devices.indexOfFirst { it.host == device.host && it.port == device.port }

        if (verifiedId != null && addressIndex >= 0) {
            val addressOwner = devices[addressIndex].deviceId
            if (addressOwner.isNotBlank() && !IdentityBindingPolicy.matches(addressOwner, verifiedId)) {
                // A different identity at an existing paired address is only an
                // unpaired candidate; it must not replace the saved endpoint.
                return false
            }
        }

        if (verifiedId == null && identityIndex < 0 && addressIndex >= 0 && devices[addressIndex].deviceId.isNotBlank()) {
            // An unverified candidate must not replace a verified device at an old address.
            return false
        }

        val existingIndex = if (identityIndex >= 0) identityIndex else addressIndex
        val existing = existingIndex.takeIf { it >= 0 }?.let { devices[it] }
        val entry = SavedDevice(
            name = device.name,
            host = if (verifiedId != null || existing?.deviceId.isNullOrBlank()) device.host else existing!!.host,
            port = if (verifiedId != null || existing?.deviceId.isNullOrBlank()) device.port else existing!!.port,
            lastConnectedAt = System.currentTimeMillis(),
            deviceId = verifiedId ?: existing?.deviceId.orEmpty(),
        )
        if (existingIndex >= 0) {
            devices[existingIndex] = entry
        } else {
            devices.add(0, entry)
        }
        saveSavedDevices(devices)
        return true
    }

    @Deprecated("Use saveDevice(DiscoveredDevice)")
    fun saveDevice(name: String, host: String, port: Int) {
        saveDevice(DiscoveredDevice(name, host, port, "http://$host:$port"))
    }

    fun getSavedDevices(): List<SavedDevice> {
        return loadSavedDevices()
    }

    fun removeSavedDevice(host: String, port: Int) {
        val devices = loadSavedDevices().filterNot { it.host == host && it.port == port }
        saveSavedDevices(devices)
    }

    fun removeSavedDevice(device: SavedDevice) {
        val devices = loadSavedDevices().filterNot {
            if (device.deviceId.isNotBlank()) it.deviceId == device.deviceId
            else it.host == device.host && it.port == device.port
        }
        clearTokenForDeviceId(device.deviceId)
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
                        lastConnectedAt = obj.optLong("lastConnectedAt", 0L),
                        deviceId = IdentityBindingPolicy.normalizeDeviceId(obj.optString("deviceId")) ?: "",
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
                    .put("deviceId", device.deviceId)
            )
        }
        prefs.edit().putString("saved_devices", array.toString()).apply()
    }

    private fun verifiedDeviceId(device: DiscoveredDevice): String? {
        val deviceId = IdentityBindingPolicy.normalizeDeviceId(device.deviceId) ?: return null
        if (!device.identityVerified || !DiscoveryEndpointPolicy.isPrivateIpv4(device.host)) return null
        return deviceId.takeIf { DiscoveryEndpointPolicy.isValidPort(device.port) }
    }

    private fun tokenKey(deviceId: String): String = "token_for_device_$deviceId"
}
