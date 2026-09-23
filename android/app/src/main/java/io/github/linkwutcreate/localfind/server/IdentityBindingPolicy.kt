package io.github.linkwutcreate.localfind.server

/**
 * Trust may follow an endpoint only after its stable identity was verified by
 * the endpoint's /device-info response.
 */
object IdentityBindingPolicy {
    fun normalizeDeviceId(deviceId: String?): String? =
        deviceId?.trim()?.takeIf { it.isNotEmpty() }

    fun matches(storedDeviceId: String?, candidateDeviceId: String?): Boolean {
        val stored = normalizeDeviceId(storedDeviceId) ?: return false
        val candidate = normalizeDeviceId(candidateDeviceId) ?: return false
        return stored == candidate
    }

    fun canTransferTrust(
        storedDeviceId: String?,
        candidateDeviceId: String?,
        candidateIdentityVerified: Boolean,
    ): Boolean = candidateIdentityVerified && matches(storedDeviceId, candidateDeviceId)
}
