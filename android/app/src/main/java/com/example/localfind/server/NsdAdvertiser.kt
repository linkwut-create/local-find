package com.example.localfind.server

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log

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
    private var registrationListener: NsdManager.RegistrationListener? = null
    
    var currentStatus: NsdStatus = NsdStatus.IDLE
        private set(value) {
            field = value
            onStatusChange(value)
        }

    val serviceType = "_localfind._tcp."
    private val serviceName = "LocalFind-${Build.MODEL}"
    private val port = 8888

    fun registerService() {
        // 先确保旧的监听器被注销
        unregisterService()

        currentStatus = NsdStatus.ADVERTISING
        
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = this@NsdAdvertiser.serviceName
            serviceType = this@NsdAdvertiser.serviceType
            port = this@NsdAdvertiser.port
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                Log.d("NsdAdvertiser", "Service registered: ${NsdServiceInfo.serviceName}")
                currentStatus = NsdStatus.ADVERTISED
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NsdAdvertiser", "Service registration failed: $errorCode")
                currentStatus = NsdStatus.FAILED
                registrationListener = null
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.d("NsdAdvertiser", "Service unregistered: ${arg0.serviceName}")
                currentStatus = NsdStatus.IDLE
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NsdAdvertiser", "Service unregistration failed: $errorCode")
                currentStatus = NsdStatus.IDLE
                registrationListener = null
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e("NsdAdvertiser", "Error during registration", e)
            currentStatus = NsdStatus.FAILED
            registrationListener = null
        }
    }

    fun unregisterService() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: Exception) {
                Log.e("NsdAdvertiser", "Error during unregistration", e)
            }
            registrationListener = null
        }
    }
}
