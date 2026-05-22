package com.example.localfind.server

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

data class DiscoveredDevice(
    val name: String,
    val host: String,
    val port: Int,
    val controlUrl: String
)

enum class DiscoveryStatus {
    IDLE,          // 未扫描
    SCANNING,      // 扫描中
    FAILED,        // 扫描失败
    STOPPED        // 已停止
}

class NsdDiscoveryManager(
    private val context: Context,
    private val onStatusChange: (DiscoveryStatus) -> Unit,
    private val onDevicesUpdate: (List<DiscoveredDevice>) -> Unit
) {
    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    
    private val discoveredDevices = mutableMapOf<String, DiscoveredDevice>()

    var currentStatus: DiscoveryStatus = DiscoveryStatus.IDLE
        private set(value) {
            field = value
            onStatusChange(value)
        }

    fun startDiscovery() {
        if (discoveryListener != null) return
        
        discoveredDevices.clear()
        onDevicesUpdate(emptyList())
        currentStatus = DiscoveryStatus.SCANNING

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d("NsdDiscovery", "Discovery started")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d("NsdDiscovery", "Service found: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceType.contains("_localfind")) {
                    resolveService(serviceInfo)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d("NsdDiscovery", "Service lost: ${serviceInfo.serviceName}")
                discoveredDevices.remove(serviceInfo.serviceName)
                onDevicesUpdate(discoveredDevices.values.toList())
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d("NsdDiscovery", "Discovery stopped")
                currentStatus = DiscoveryStatus.STOPPED
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NsdDiscovery", "Discovery failed: $errorCode")
                currentStatus = DiscoveryStatus.FAILED
                discoveryListener = null
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NsdDiscovery", "Stop discovery failed: $errorCode")
                discoveryListener = null
            }
        }

        try {
            nsdManager.discoverServices("_localfind._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e("NsdDiscovery", "Error starting discovery", e)
            currentStatus = DiscoveryStatus.FAILED
            discoveryListener = null
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NsdDiscovery", "Resolve failed: $errorCode")
            }

            override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                Log.d("NsdDiscovery", "Resolve succeeded: ${resolvedServiceInfo.serviceName}")
                val host = resolvedServiceInfo.host?.hostAddress ?: "Unknown"
                val port = resolvedServiceInfo.port
                val device = DiscoveredDevice(
                    name = resolvedServiceInfo.serviceName,
                    host = host,
                    port = port,
                    controlUrl = "http://$host:$port"
                )
                discoveredDevices[device.name] = device
                onDevicesUpdate(discoveredDevices.values.toList())
            }
        }
        
        try {
            nsdManager.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e("NsdDiscovery", "Error resolving service", e)
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e("NsdDiscovery", "Error stopping discovery", e)
            }
            discoveryListener = null
        }
    }
}
