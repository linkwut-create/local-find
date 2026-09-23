package io.github.linkwutcreate.localfind.server

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.net.wifi.WifiManager

enum class NsdStatus {
    IDLE,          // 未广播
    ADVERTISING,   // 广播中
    ADVERTISED,    // 已广播
    FAILED         // 广播失败
}

class NsdAdvertiser(
    private val context: Context,
    private val onStatusChange: (NsdStatus) -> Unit
) {
    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val handler = Handler(Looper.getMainLooper())
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var registrationGeneration = 0L
    private var retryAttempt = 0
    private var shouldAdvertise = false
    private var multicastLock: WifiManager.MulticastLock? = null
    
    var currentStatus: NsdStatus = NsdStatus.IDLE
        private set(value) {
            field = value
            onStatusChange(value)
        }

    // Android's NSD API expects the service type without the DNS root dot.
    // The wire-format mDNS name still becomes _localfind._tcp.local.
    val serviceType = "_localfind._tcp"
    private val serviceName = "LocalFind-${Build.MODEL}"
    private val port = 8888

    fun registerService() {
        shouldAdvertise = true
        retryAttempt = 0
        acquireMulticastLock()
        val generation = ++registrationGeneration
        val hadPreviousRegistration = registrationListener != null
        unregisterCurrentListener()
        currentStatus = NsdStatus.ADVERTISING
        scheduleRegistration(generation, if (hadPreviousRegistration) 750L else 0L)
    }

    fun unregisterService() {
        shouldAdvertise = false
        ++registrationGeneration
        handler.removeCallbacksAndMessages(null)
        unregisterCurrentListener()
        releaseMulticastLock()
        currentStatus = NsdStatus.IDLE
    }

    private fun scheduleRegistration(generation: Long, delayMs: Long) {
        handler.postDelayed({
            if (shouldAdvertise && generation == registrationGeneration && registrationListener == null) {
                registerNow(generation)
            }
        }, delayMs)
    }

    private fun registerNow(generation: Long) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = this@NsdAdvertiser.serviceName
            serviceType = this@NsdAdvertiser.serviceType
            port = this@NsdAdvertiser.port
            // Android 16's NSD resolver rejects an empty TXT entry with
            // "Key cannot be empty". Keep one valid marker in the record.
            setAttribute("service", "localfind")
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(registeredInfo: NsdServiceInfo) {
                if (generation != registrationGeneration || !shouldAdvertise) return
                Log.d("NsdAdvertiser", "Service registered: ${registeredInfo.serviceName}")
                retryAttempt = 0
                currentStatus = NsdStatus.ADVERTISED
            }

            override fun onRegistrationFailed(failedInfo: NsdServiceInfo, errorCode: Int) {
                if (generation != registrationGeneration || !shouldAdvertise) return
                Log.e("NsdAdvertiser", "Service registration failed: $errorCode")
                registrationListener = null
                currentStatus = NsdStatus.FAILED
                val delay = (1000L shl retryAttempt.coerceAtMost(4)).coerceAtMost(16000L)
                retryAttempt += 1
                scheduleRegistration(generation, delay)
            }

            override fun onServiceUnregistered(unregisteredInfo: NsdServiceInfo) {
                if (generation != registrationGeneration) return
                Log.d("NsdAdvertiser", "Service unregistered: ${unregisteredInfo.serviceName}")
                registrationListener = null
                if (shouldAdvertise) {
                    currentStatus = NsdStatus.ADVERTISING
                    scheduleRegistration(generation, 750L)
                } else {
                    currentStatus = NsdStatus.IDLE
                }
            }

            override fun onUnregistrationFailed(failedInfo: NsdServiceInfo, errorCode: Int) {
                if (generation != registrationGeneration) return
                Log.e("NsdAdvertiser", "Service unregistration failed: $errorCode")
                registrationListener = null
                if (shouldAdvertise) {
                    currentStatus = NsdStatus.ADVERTISING
                    scheduleRegistration(generation, 1500L)
                } else {
                    currentStatus = NsdStatus.IDLE
                }
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e("NsdAdvertiser", "Error during registration", e)
            registrationListener = null
            currentStatus = NsdStatus.FAILED
            if (shouldAdvertise && generation == registrationGeneration) {
                val delay = (1000L shl retryAttempt.coerceAtMost(4)).coerceAtMost(16000L)
                retryAttempt += 1
                scheduleRegistration(generation, delay)
            }
        }
    }

    private fun unregisterCurrentListener() {
        registrationListener?.let { listener ->
            try {
                nsdManager.unregisterService(listener)
            } catch (e: Exception) {
                Log.e("NsdAdvertiser", "Error during unregistration", e)
            }
        }
        registrationListener = null
    }

    @Suppress("DEPRECATION")
    private fun acquireMulticastLock() {
        if (multicastLock == null) {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            multicastLock = wifiManager.createMulticastLock("LocalFind:NsdMulticast")
                .apply { setReferenceCounted(false) }
        }
        multicastLock?.let { lock ->
            if (!lock.isHeld) {
                lock.acquire()
                Log.d("NsdAdvertiser", "Multicast lock acquired")
            }
        }
    }

    private fun releaseMulticastLock() {
        multicastLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
                Log.d("NsdAdvertiser", "Multicast lock released")
            }
        }
        multicastLock = null
    }
}
