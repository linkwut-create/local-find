package io.github.linkwutcreate.localfind.server

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class DiscoveredDevice(
    val name: String,
    val host: String,
    val port: Int,
    val controlUrl: String,
    val deviceId: String = "",
    /** True only after this endpoint's /device-info response was verified. */
    val identityVerified: Boolean = false,
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
    private val verificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var discoveryGeneration = 0L
    
    private val discoveredDevices = mutableMapOf<String, DiscoveredDevice>()
    private val serviceNameToDeviceId = mutableMapOf<String, String>()

    var currentStatus: DiscoveryStatus = DiscoveryStatus.IDLE
        private set(value) {
            field = value
            onStatusChange(value)
        }

    fun startDiscovery() {
        if (discoveryListener != null) return

        val generation = ++discoveryGeneration
        
        discoveredDevices.clear()
        serviceNameToDeviceId.clear()
        onDevicesUpdate(emptyList())
        currentStatus = DiscoveryStatus.SCANNING

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d("NsdDiscovery", "Discovery started")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d("NsdDiscovery", "Service found: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceType.contains("_localfind")) {
                    resolveService(serviceInfo, generation)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d("NsdDiscovery", "Service lost: ${serviceInfo.serviceName}")
                serviceNameToDeviceId.remove(serviceInfo.serviceName)?.let { deviceId ->
                    discoveredDevices.remove(deviceId)
                }
                onDevicesUpdate(discoveredDevices.values.toList())
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d("NsdDiscovery", "Discovery stopped")
                currentStatus = DiscoveryStatus.STOPPED
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                val errorMsg = when(errorCode) {
                    NsdManager.FAILURE_ALREADY_ACTIVE -> "扫描已在运行中"
                    NsdManager.FAILURE_INTERNAL_ERROR -> "系统 NSD 内部错误"
                    NsdManager.FAILURE_MAX_LIMIT -> "达到系统 NSD 监听限制"
                    else -> "扫描启动失败 (错误代码: $errorCode)"
                }
                Log.e("NsdDiscovery", errorMsg)
                currentStatus = DiscoveryStatus.FAILED
                discoveryListener = null
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NsdDiscovery", "Stop discovery failed: $errorCode")
                discoveryListener = null
            }
        }

        try {
            nsdManager.discoverServices("_localfind._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e("NsdDiscovery", "Error starting discovery", e)
            currentStatus = DiscoveryStatus.FAILED
            discoveryListener = null
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo, generation: Long) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NsdDiscovery", "Resolve failed: $errorCode")
            }

            @Suppress("DEPRECATION")
            override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                Log.d("NsdDiscovery", "Resolve succeeded: ${resolvedServiceInfo.serviceName}")
                // host/hostAddress deprecated API 34; migrate to hostAddresses when minSdk >= 33
                val host = resolvedServiceInfo.host?.hostAddress ?: "Unknown"
                val port = resolvedServiceInfo.port
                verificationScope.launch {
                    val device = verifyResolvedEndpoint(
                        serviceName = resolvedServiceInfo.serviceName,
                        host = host,
                        port = port,
                    ) ?: return@launch
                    if (generation != discoveryGeneration || discoveryListener == null) return@launch
                    withContext(Dispatchers.Main.immediate) {
                        if (generation != discoveryGeneration || discoveryListener == null) return@withContext
                        discoveredDevices[device.deviceId] = device
                        serviceNameToDeviceId[resolvedServiceInfo.serviceName] = device.deviceId
                        onDevicesUpdate(discoveredDevices.values.toList())
                    }
                }
            }
        }
        
        try {
            // resolveService deprecated API 34; migrate to registerServiceInfoCallback when minSdk >= 34
            @Suppress("DEPRECATION")
            nsdManager.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e("NsdDiscovery", "Error resolving service", e)
        }
    }

    fun stopDiscovery() {
        ++discoveryGeneration
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e("NsdDiscovery", "Error stopping discovery", e)
            }
            discoveryListener = null
        }
    }

    private suspend fun verifyResolvedEndpoint(
        serviceName: String,
        host: String,
        port: Int,
    ): DiscoveredDevice? = withContext(Dispatchers.IO) {
        if (!DiscoveryEndpointPolicy.isPrivateIpv4(host)) {
            Log.w("NsdDiscovery", "Ignoring non-private NSD endpoint: $host")
            return@withContext null
        }
        val verifiedPort = DiscoveryEndpointPolicy.validPort(port) ?: return@withContext null
        val connection = try {
            (URL("http://$host:$verifiedPort/device-info").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 1500
                readTimeout = 1500
                useCaches = false
            }
        } catch (error: Exception) {
            Log.w("NsdDiscovery", "Unable to connect to NSD endpoint $host:$verifiedPort", error)
            return@withContext null
        }

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val deviceId = IdentityBindingPolicy.normalizeDeviceId(json.optString("id"))
                ?: return@withContext null
            if (json.optString("service").trim() != "running") return@withContext null
            DiscoveredDevice(
                name = json.optString("name").ifBlank { serviceName },
                host = host,
                port = verifiedPort,
                controlUrl = "http://$host:$verifiedPort",
                deviceId = deviceId,
                identityVerified = true,
            )
        } catch (error: Exception) {
            Log.w("NsdDiscovery", "Invalid /device-info response from $host:$verifiedPort", error)
            null
        } finally {
            connection.disconnect()
        }
    }
}
