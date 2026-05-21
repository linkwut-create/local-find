package com.example.localfind.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.localfind.MainActivity
import com.example.localfind.hardware.FlashlightController
import com.example.localfind.hardware.RingController
import com.example.localfind.server.HttpServerManager
import com.example.localfind.server.NsdAdvertiser
import com.example.localfind.server.NsdStatus

class FindPhoneForegroundService : Service() {

    private val binder = LocalBinder()
    
    private lateinit var ringController: RingController
    private lateinit var flashlightController: FlashlightController
    private var httpServerManager: HttpServerManager? = null
    private var nsdAdvertiser: NsdAdvertiser? = null

    // 观察者，用于将状态回调投射至 Activity 活动页面
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
        
        httpServerManager = HttpServerManager(ringController, flashlightController) {
            onStatusChangeListener?.invoke()
        }

        nsdAdvertiser = NsdAdvertiser(this) {
            onStatusChangeListener?.invoke()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ForegroundService", "Starting FindPhoneForegroundService")
        createNotificationChannel()
        val notification = createNotification()
        
        try {
            // Android 14 (API 34) 严格要求前台服务声明符合 manifest 对应的 type 类型
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("ForegroundService", "Failed to start service as foreground explicitly, falling back", e)
            startForeground(NOTIFICATION_ID, notification)
        }

        httpServerManager?.start()
        nsdAdvertiser?.registerService()
        onStatusChangeListener?.invoke()
        
        // START_STICKY 保证因系统内存不足被杀后，系统有机会重建服务
        return START_STICKY
    }

    /**
     * 关闭服务
     */
    fun stopService() {
        httpServerManager?.stop()
        nsdAdvertiser?.unregisterService()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        httpServerManager?.stop()
        nsdAdvertiser?.unregisterService()
        Log.d("ForegroundService", "Service onDestroy finished")
        super.onDestroy()
    }

    fun isRingActive(): Boolean = httpServerManager?.isRingActive ?: false
    fun getFlashMode(): String = httpServerManager?.flashMode ?: "off"
    fun isServerRunning(): Boolean = httpServerManager != null
    fun getNsdStatus(): NsdStatus = nsdAdvertiser?.currentStatus ?: NsdStatus.IDLE
    fun getNsdServiceType(): String = nsdAdvertiser?.serviceType ?: "_localfind._tcp."

    fun setStatusChangeListener(listener: (() -> Unit)?) {
        this.onStatusChangeListener = listener
    }

    // 触发本端操作
    fun triggerLocalRing() {
        try {
            httpServerManager?.start() // 保证状态正确
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    }

    private fun createNotification(): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Local Find 寻机服务")
            .setContentText("Local Find 正在本地网络中等待你的设备指令")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "LocalFindForegroundChannel"
        const val NOTIFICATION_ID = 2026
    }
}
