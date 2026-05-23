package com.example.localfind.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.localfind.MainActivity
import com.example.localfind.auth.PairingTokenManager
import com.example.localfind.hardware.FlashlightController
import com.example.localfind.hardware.RingController
import com.example.localfind.model.LocalDeviceIdentity
import com.example.localfind.model.PairingRequest
import com.example.localfind.server.HttpServerManager
import com.example.localfind.server.NsdAdvertiser
import com.example.localfind.server.NsdStatus
import com.example.localfind.server.ServerStatus
import com.example.localfind.store.LocalDeviceIdentityStore
import com.example.localfind.store.PairedControllerTokenStore
import com.example.localfind.store.PairingRequestStore
import com.example.localfind.util.NetworkUtil

class FindPhoneForegroundService : Service() {

    private val binder = LocalBinder()
    
    private lateinit var ringController: RingController
    private lateinit var flashlightController: FlashlightController
    private var httpServerManager: HttpServerManager? = null
    private var nsdAdvertiser: NsdAdvertiser? = null
    private lateinit var pairingTokenManager: PairingTokenManager
    private lateinit var localDeviceIdentityStore: LocalDeviceIdentityStore
    private lateinit var pairingRequestStore: PairingRequestStore
    private lateinit var pairedControllerTokenStore: PairedControllerTokenStore
    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private val watchdogHandler = Handler(Looper.getMainLooper())
    private val watchdogRunnable = object : Runnable {
        override fun run() {
            checkAndRecoveryServer()
            watchdogHandler.postDelayed(this, 15000) // 15 seconds
        }
    }

    private var currentIp: String? = null

    // Observer to notify Activity of state changes
    private var onStatusChangeListener: (() -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): FindPhoneForegroundService = this@FindPhoneForegroundService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("ForegroundService", "Service onCreate")
        ringController = RingController(this)
        flashlightController = FlashlightController(this)
        pairingTokenManager = PairingTokenManager(this)
        localDeviceIdentityStore = LocalDeviceIdentityStore(this)
        pairingRequestStore = PairingRequestStore(this)
        pairedControllerTokenStore = PairedControllerTokenStore(this)
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        httpServerManager = HttpServerManager(
            ringController,
            flashlightController,
            pairingTokenManager,
            localDeviceIdentityStore,
            pairingRequestStore,
            pairedControllerTokenStore,
        ) {
            onStatusChangeListener?.invoke()
        }

        nsdAdvertiser = NsdAdvertiser(this) {
            onStatusChangeListener?.invoke()
        }

        currentIp = NetworkUtil.getLocalIpAddress()
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                checkAndUpdateIp()
            }

            override fun onLost(network: Network) {
                checkAndUpdateIp()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                checkAndUpdateIp()
            }
        }
        
        networkCallback?.let {
            connectivityManager.registerNetworkCallback(networkRequest, it)
        }
    }

    private fun checkAndUpdateIp() {
        val newIp = NetworkUtil.getLocalIpAddress()
        if (newIp != currentIp) {
            Log.d("ForegroundService", "IP changed from $currentIp to $newIp")
            currentIp = newIp
            
            // Re-register NSD when IP changes
            if (isServerRunning()) {
                nsdAdvertiser?.unregisterService()
                nsdAdvertiser?.registerService()
            }
            
            updateNotification()
            onStatusChangeListener?.invoke()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ForegroundService", "Starting FindPhoneForegroundService")
        
        // Start foreground immediately for high priority
        createNotificationChannel()
        val notification = createNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("ForegroundService", "Failed to start foreground", e)
        }

        acquireWakeLock()
        acquireWifiLock()
        startWatchdog()

        currentIp = NetworkUtil.getLocalIpAddress()
        
        httpServerManager?.start()
        nsdAdvertiser?.registerService()
        onStatusChangeListener?.invoke()
        
        // START_STICKY ensures service recreation after OOM kills
        return START_STICKY
    }

    /**
     * Shuts down the service and cleans up all resources.
     */
    fun stopService() {
        stopWatchdog()
        httpServerManager?.shutdownAll()
        nsdAdvertiser?.unregisterService()
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
        releaseWakeLock()
        releaseWifiLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopWatchdog()
        httpServerManager?.shutdownAll()
        nsdAdvertiser?.unregisterService()
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {}
        }
        releaseWakeLock()
        releaseWifiLock()
        Log.d("ForegroundService", "Service onDestroy finished")
        super.onDestroy()
    }

    private fun startWatchdog() {
        watchdogHandler.removeCallbacks(watchdogRunnable)
        watchdogHandler.postDelayed(watchdogRunnable, 15000)
        Log.d("ForegroundService", "Watchdog started")
    }

    private fun stopWatchdog() {
        watchdogHandler.removeCallbacks(watchdogRunnable)
        Log.d("ForegroundService", "Watchdog stopped")
    }

    private fun checkAndRecoveryServer() {
        val status = httpServerManager?.currentStatus ?: ServerStatus.STOPPED
        if ((status == ServerStatus.FAILED) || (status == ServerStatus.STOPPED)) {
            Log.w("ForegroundService", "Watchdog detected server down ($status), attempting restart...")
            httpServerManager?.restart()
        } else {
            Log.d("ForegroundService", "Watchdog check: Server is $status")
            // Periodically trigger expiration check and UI sync
            httpServerManager?.isPairingModeActive()
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocalFind:ServiceWakeLock")
        }
        wakeLock?.apply {
            if (!isHeld) {
                acquire(24 * 60 * 60 * 1000L /* 24 hours safety timeout */)
                Log.d("ForegroundService", "WakeLock (Partial) acquired")
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.apply {
            if (isHeld) {
                release()
                Log.d("ForegroundService", "WakeLock released")
            }
        }
        wakeLock = null
    }

    private fun acquireWifiLock() {
        if (wifiLock == null) {
            val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "LocalFind:WifiLock")
            } else {
                @Suppress("DEPRECATION")
                wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "LocalFind:WifiLock")
            }
        }
        wifiLock?.apply {
            if (!isHeld) {
                acquire()
                Log.d("ForegroundService", "WifiLock acquired")
            }
        }
    }

    private fun releaseWifiLock() {
        wifiLock?.apply {
            if (isHeld) {
                release()
                Log.d("ForegroundService", "WifiLock released")
            }
        }
        wifiLock = null
    }

    fun getLocalIp(): String? = currentIp

    fun isRingActive(): Boolean = httpServerManager?.isRingActive ?: false
    fun getFlashMode(): String = httpServerManager?.flashMode ?: "off"
    fun isServerRunning(): Boolean = httpServerManager?.currentStatus != ServerStatus.STOPPED
    fun getServerStatus(): ServerStatus = httpServerManager?.currentStatus ?: ServerStatus.STOPPED
    fun getLastServerError(): String? = httpServerManager?.lastServerError
    fun restartServer() = httpServerManager?.restart()

    fun isWakeLockHeld(): Boolean = wakeLock?.isHeld ?: false
    fun isWifiLockHeld(): Boolean = wifiLock?.isHeld ?: false

    fun getNsdStatus(): NsdStatus = nsdAdvertiser?.currentStatus ?: NsdStatus.IDLE
    fun getNsdServiceType(): String = nsdAdvertiser?.serviceType ?: "_localfind._tcp."

    fun getPairingToken(): String = pairingTokenManager.getToken() ?: ""
    fun regeneratePairingToken(): String {
        val newToken = pairingTokenManager.regenerateToken()
        onStatusChangeListener?.invoke()
        return newToken
    }

    fun getLocalDeviceIdentity(): LocalDeviceIdentity = localDeviceIdentityStore.getOrCreate()
    fun isPairingModeActive(): Boolean = httpServerManager?.isPairingModeActive() ?: false
    fun getPairingModeExpiresAt(): Long = httpServerManager?.getPairingModeExpiresAt() ?: 0L
    fun getPendingPairingRequests(): List<PairingRequest> = httpServerManager?.getPendingPairingRequests().orEmpty()

    fun enablePairingMode() {
        httpServerManager?.enablePairingMode()
    }

    fun disablePairingMode() {
        httpServerManager?.disablePairingMode()
    }

    fun acceptPairingRequest(requestId: String) {
        httpServerManager?.acceptPairingRequest(requestId)
    }

    fun rejectPairingRequest(requestId: String) {
        httpServerManager?.rejectPairingRequest(requestId)
    }

    fun setStatusChangeListener(listener: (() -> Unit)?) {
        this.onStatusChangeListener = listener
    }

    // Trigger local hardware actions
    fun triggerLocalRing() {
        try {
            httpServerManager?.start() 
        } catch(e: Exception){}
        ringController.startRing()
        onStatusChangeListener?.invoke()
    }
    
    fun triggerLocalRingStop() {
        ringController.stopRing()
        onStatusChangeListener?.invoke()
    }

    fun triggerLocalFlashSteady() {
        flashlightController.startSteady()
        onStatusChangeListener?.invoke()
    }

    fun triggerLocalFlashStrobe() {
        flashlightController.startStrobe()
        onStatusChangeListener?.invoke()
    }

    fun triggerLocalFlashStop() {
        flashlightController.stopAll()
        onStatusChangeListener?.invoke()
    }

    fun stopAll() {
        ringController.stopRing()
        flashlightController.stopAll()
        onStatusChangeListener?.invoke()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Local Find Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the Local Find HTTP server active in the background"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val ipText = currentIp ?: "No Wi-Fi"
        val contentText = "Running | IP: $ipText | Port: 8888"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Local Find Search Service")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    companion object {
        const val CHANNEL_ID = "LocalFindForegroundChannel"
        const val NOTIFICATION_ID = 2026
    }
}
