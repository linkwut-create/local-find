package io.github.linkwutcreate.localfind.server

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.linkwutcreate.localfind.store.LocalDeviceIdentityStore
import io.github.linkwutcreate.localfind.util.NetworkUtil
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Periodically announces the phone's current LAN endpoint. The payload has no
 * control token; the desktop bridge still verifies the persistent device ID
 * through /device-info before returning an address to the extension.
 */
class UdpDiscoveryAnnouncer(
    private val context: Context,
    private val identityStore: LocalDeviceIdentityStore,
    private val onAnnouncement: (() -> Unit)? = null,
) {
    companion object {
        const val PORT = 43790
        private const val INTERVAL_MILLIS = 5000L
        private const val TAG = "UdpDiscoveryAnnouncer"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var executor: ExecutorService? = null
    private var running = false
    private val announceRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            announceNow()
            handler.postDelayed(this, INTERVAL_MILLIS)
        }
    }

    @Synchronized
    fun start() {
        if (running) return
        running = true
        executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "LocalFind-DiscoveryBeacon").apply { isDaemon = true }
        }
        announceNow()
        handler.postDelayed(announceRunnable, INTERVAL_MILLIS)
    }

    @Synchronized
    fun announceNow() {
        if (!running) return
        executor?.execute { sendAnnouncement() }
    }

    @Synchronized
    fun stop() {
        running = false
        handler.removeCallbacks(announceRunnable)
        executor?.shutdownNow()
        executor = null
    }

    private fun sendAnnouncement() {
        val network = NetworkUtil.getLocalNetworkInfo() ?: return
        if (!network.interfaceName.startsWith("wlan") && !network.interfaceName.startsWith("eth")) {
            // Never emit a LAN discovery beacon through a cellular interface.
            return
        }
        val identity = identityStore.getOrCreate()
        val payload = JSONObject()
            .put("id", identity.id)
            .put("name", identity.name)
            .put("type", identity.type)
            .put("host", network.address)
            .put("port", 8888)
            .put("networkPrefixLength", network.prefixLength)
            .put("service", "localfind")
            .toString()
            .toByteArray(Charsets.UTF_8)

        val broadcast = getBroadcastAddress(network.address, network.prefixLength) ?: return
        try {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.send(DatagramPacket(payload, payload.size, broadcast, PORT))
            }
            onAnnouncement?.invoke()
        } catch (error: Exception) {
            Log.w(TAG, "Unable to send discovery announcement", error)
        }
    }

    private fun getBroadcastAddress(address: String, prefixLength: Int): InetAddress? {
        val octets = address.split('.').mapNotNull { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it !in 0..255 }) return null
        val prefix = prefixLength.coerceIn(16, 30)
        val ip = ((octets[0] shl 24) or (octets[1] shl 16) or (octets[2] shl 8) or octets[3])
        val mask = (-1 shl (32 - prefix))
        val broadcast = (ip and mask) or mask.inv()
        val bytes = byteArrayOf(
            (broadcast ushr 24).toByte(),
            (broadcast ushr 16).toByte(),
            (broadcast ushr 8).toByte(),
            broadcast.toByte(),
        )
        return InetAddress.getByAddress(bytes)
    }
}
