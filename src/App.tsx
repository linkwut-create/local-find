import { useState, useEffect, useRef } from "react";
import { 
  Phone, 
  Wifi, 
  Server, 
  Terminal, 
  FileCode, 
  Volume2, 
  Copy, 
  Check, 
  Play, 
  Square, 
  Radio, 
  Smartphone, 
  Info, 
  VolumeX, 
  Zap, 
  Code2, 
  FileText, 
  BookOpen, 
  RefreshCw,
  FolderOpen
} from "lucide-react";

// Raw File code repositories for the Android project
const ANDROID_FILES = [
  {
    name: "AndroidManifest.xml",
    path: "/android/app/src/main/AndroidManifest.xml",
    role: "基础清单：申请相机、网络、前台服务特殊权限，并声明支持后台寻机服务的类型。",
    lang: "xml",
    code: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.localfind">

    <!-- 网络请求和建服核心权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    
    <!-- 控制电筒闪烁必须要相机硬件调用权限 -->
    <uses-permission android:name="android.permission.CAMERA" />
    
    <!-- Android 13+ 常驻状态栏通知推送权限 -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <!-- Android 9+ 前台服务常驻内存凭证 -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    
    <!-- Android 14+ 严格规定必须标示 SpecialUse 特殊用途类型前台 -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

    <application
        android:allowBackup="true"
        android:icon="@android:drawable/ic_menu_search"
        android:label="Local Find"
        android:roundIcon="@android:drawable/ic_menu_search"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.DeviceDefault.NoActionBar">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@android:style/Theme.DeviceDefault.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- 前台寻机监听守护服务，声明特别属性和解释 -->
        <service
            android:name=".service.FindPhoneForegroundService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="Find lost phone controlled via local Wi-Fi HTTP API" />
        </service>
        
    </application>

</manifest>`
  },
  {
    name: "MainActivity.kt",
    path: "/android/app/src/main/java/com/example/localfind/MainActivity.kt",
    role: "入口Activity：生命周期、动态权限请求、绑定后台前台服务、建立状态监听器，实现内外命令UI状态实时同步。",
    lang: "kotlin",
    code: `package com.example.localfind

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.localfind.service.FindPhoneForegroundService
import com.example.localfind.ui.MainScreen
import com.example.localfind.util.NetworkUtil

class MainActivity : ComponentActivity() {

    private var foregroundService: FindPhoneForegroundService? = null
    private var isServiceBound by mutableStateOf(false)

    // 反射给 Jetpack Compose 响应式核心状态
    private var ringActiveState by mutableStateOf(false)
    private var flashModeState by mutableStateOf("off")
    private var isServiceRunningState by mutableStateOf(false)
    private var deviceIpState by mutableStateOf<String?>(null)

    // 连接后台前台服务的 Connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as FindPhoneForegroundService.LocalBinder
            val boundService = binder.getService()
            foregroundService = boundService
            isServiceBound = true
            isServiceRunningState = true
            
            // 同步一次当前的各种硬件外设状态
            syncServiceStatus()

            // 监听服务状态变动的回调，实现远程/外部触发后，App 界面可以同步瞬间改变
            boundService.setStatusChangeListener {
                syncServiceStatus()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            foregroundService = null
            isServiceRunningState = false
        }
    }

    private fun syncServiceStatus() {
        foregroundService?.let { service ->
            ringActiveState = service.isRingActive()
            flashModeState = service.getFlashMode()
            isServiceRunningState = service.isServerRunning()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // 权限请求完毕，不阻断，用户自主使用
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 分配本机无线 IP
        deviceIpState = NetworkUtil.getLocalIpAddress()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        isServiceRunning = isServiceRunningState,
                        localIp = deviceIpState,
                        port = 8888,
                        ringActive = ringActiveState,
                        flashMode = flashModeState,
                        onStartService = { startAndBindService() },
                        onStopService = { shutdownService() },
                        onTestRingToggle = {
                            if (isServiceBound) {
                                if (ringActiveState) {
                                    foregroundService?.triggerLocalRingStop()
                                } else {
                                    foregroundService?.triggerLocalRing()
                                }
                            }
                        },
                        onTestFlashSteady = {
                            if (isServiceBound) {
                                foregroundService?.triggerLocalFlashSteady()
                            }
                        },
                        onTestFlashStrobe = {
                            if (isServiceBound) {
                                foregroundService?.triggerLocalFlashStrobe()
                            }
                        },
                        onTestFlashStop = {
                            if (isServiceBound) {
                                foregroundService?.triggerLocalFlashStop()
                            }
                        },
                        onStopAll = {
                            if (isServiceBound) {
                                foregroundService?.stopAll()
                            }
                        },
                        onRequestPermission = { checkAndRequestPermissions() }
                    )
                }
            }
        }

        checkAndRequestPermissions()
        refreshServiceStatus()
    }

    private fun refreshServiceStatus() {
        deviceIpState = NetworkUtil.getLocalIpAddress()
        try {
            bindToService()
        } catch (_: Exception) {}
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startAndBindService() {
        deviceIpState = NetworkUtil.getLocalIpAddress()
        val serviceIntent = Intent(this, FindPhoneForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        bindToService()
    }

    private fun bindToService() {
        val serviceIntent = Intent(this, FindPhoneForegroundService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun shutdownService() {
        if (isServiceBound) {
            foregroundService?.stopService()
            unbindService(serviceConnection)
            isServiceBound = false
            foregroundService = null
        }
        isServiceRunningState = false
        ringActiveState = false
        flashModeState = "off"
    }

    override fun onResume() {
        super.onResume()
        refreshServiceStatus()
    }

    override fun onDestroy() {
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        super.onDestroy()
    }
}`
  },
  {
    name: "MainScreen.kt",
    path: "/android/app/src/main/java/com/example/localfind/ui/MainScreen.kt",
    role: "Compose视觉框架：绘制状态看板、物理报警单元测卡、卡片渐变组件和终端操作命令提示。",
    lang: "kotlin",
    code: `package com.example.localfind.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isServiceRunning: Boolean,
    localIp: String?,
    port: Int,
    ringActive: Boolean,
    flashMode: String,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onTestRingToggle: () -> Unit,
    onTestFlashSteady: () -> Unit,
    onTestFlashStrobe: () -> Unit,
    onTestFlashStop: () -> Unit,
    onStopAll: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Local Find 被寻找端",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            val statusColor by animateColorAsState(
                targetValue = if (isServiceRunning) Color(0xFF4CAF50) else Color(0xFFF44336),
                label = "statusColor"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "服务状态",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = statusColor,
                            modifier = Modifier.size(12.dp)
                        ) {}
                        Text(
                            text = if (isServiceRunning) "正在监听 (Running)" else "未运行 (Stopped)",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isServiceRunning) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("当前手机局域网 IP:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = localIp ?: "Wi-Fi 未连接",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (localIp != null) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("监听端口:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = port.toString(),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isServiceRunning && localIp != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "外部控制基准：http://$localIp:$port",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Controls Section (Service Toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { 
                        onRequestPermission()
                        onStartService() 
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = !isServiceRunning
                ) {
                    Text("启动寻机服务", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onStopService,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = isServiceRunning
                ) {
                    Text("停止寻机服务", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hardware Action Controls (Both for testing locally)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "硬件寻机外设测试 (本地)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Alarm status indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (ringActive) Color(0xFFFFEB3B).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("音频响铃", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = if (ringActive) "鸣叫中" else "静音",
                                    fontWeight = FontWeight.Bold,
                                    color = if (ringActive) Color(0xFFE65100) else Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (flashMode != "off") Color(0xFFFFEB3B).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("手电筒闪光", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = when(flashMode) {
                                        "steady" -> "常亮开启"
                                        "strobe" -> "闪烁爆闪"
                                        else -> "已关闭"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = if (flashMode != "off") Color(0xFFE65100) else Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Test Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTestRingToggle,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (ringActive) "停止响铃" else "测试响铃", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onTestFlashSteady,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("测试常亮", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTestFlashStrobe,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("测试闪烁", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onTestFlashStop,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("关闭手电", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = onStopAll,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("一键停止全部动作", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Client instruction help box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "电脑端局域网调用测试 (curl)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "请在电脑终端直接运行以下 curl 命令来触发寻机声光警示：",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "curl -X POST http://\${localIp ?: "192.168.x.x"}:8888/command/ring/start",
                            modifier = Modifier.padding(8.dp),
                            color = Color(0xFF00FF00),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
`
  },
  {
    name: "FindPhoneForegroundService.kt",
    path: "/android/app/src/main/java/com/example/localfind/service/FindPhoneForegroundService.kt",
    role: "前台服务：常驻通知生命周期、指定 specialUse 运行类别。作为桥梁，拉起 Ktor Server 并将物理硬件指令与 Activity 回调双向打通。",
    lang: "kotlin",
    code: `package com.example.localfind.service

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

class FindPhoneForegroundService : Service() {

    private val binder = LocalBinder()
    
    private lateinit var ringController: RingController
    private lateinit var flashlightController: FlashlightController
    private var httpServerManager: HttpServerManager? = null

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
        onStatusChangeListener?.invoke()
        
        // START_STICKY 保证因系统内存不足被杀后，系统有机会重建服务
        return START_STICKY
    }

    /**
     * 关闭服务
     */
    fun stopService() {
        httpServerManager?.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        httpServerManager?.stop()
        Log.d("ForegroundService", "Service onDestroy finished")
        super.onDestroy()
    }

    fun isRingActive(): Boolean = httpServerManager?.isRingActive ?: false
    fun getFlashMode(): String = httpServerManager?.flashMode ?: "off"
    fun isServerRunning(): Boolean = httpServerManager != null

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
}`
  },
  {
    name: "HttpServerManager.kt",
    path: "/android/app/src/main/java/com/example/localfind/server/HttpServerManager.kt",
    role: "Ktor Web服务端：内嵌 Netty，监听 0.0.0.0:8888 本地端口，序列化解析指令并映射、反射和调用对硬件各功能的控制。",
    lang: "kotlin",
    code: `package com.example.localfind.server

import android.util.Log
import com.example.localfind.hardware.FlashlightController
import com.example.localfind.hardware.RingController
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class HttpServerManager(
    private val ringController: RingController,
    private val flashlightController: FlashlightController,
    private val onStatusChange: () -> Unit
) {
    private var server: NettyApplicationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    var isRingActive = false
        private set
    var flashMode: String = "off" // "off", "steady", "strobe"
        private set

    fun getPort(): Int = 8888

    /**
     * 启动本地 Ktor Web 服务，监听端口 8888
     */
    @Synchronized
    fun start() {
        if (server != null) return
        
        scope.launch {
            try {
                server = embeddedServer(Netty, port = getPort(), host = "0.0.0.0") {
                    install(ContentNegotiation) {
                        json()
                    }
                    routing {
                        // 1. GET /status  获取服务和外设的实时运行姿态
                        get("/status") {
                            val statusJson = buildJsonObject {
                                put("service", "running")
                                put("ring_active", isRingActive)
                                put("flash_mode", flashMode)
                            }
                            call.respond(statusJson)
                        }

                        // 2. POST /command/ring/start  开始循环拉响警报音
                        post("/command/ring/start") {
                            isRingActive = true
                            ringController.startRing()
                            onStatusChange()
                            call.respond(buildJsonObject { 
                                put("success", true)
                                put("message", "Ring started") 
                            })
                        }

                        // 3. POST /command/ring/stop  停止警报音
                        post("/command/ring/stop") {
                            isRingActive = false
                            ringController.stopRing()
                            onStatusChange()
                            call.respond(buildJsonObject { 
                                put("success", true)
                                put("message", "Ring stopped") 
                            })
                        }

                        // 4. POST /command/flash/steady/start  开启手电筒常亮
                        post("/command/flash/steady/start") {
                            flashMode = "steady"
                            flashlightController.startSteady()
                            onStatusChange()
                            call.respond(buildJsonObject { 
                                put("success", true)
                                put("message", "Steady flashlight started") 
                            })
                        }

                        // 5. POST /command/flash/strobe/start  开启 200ms 的爆闪
                        post("/command/flash/strobe/start") {
                            flashMode = "strobe"
                            flashlightController.startStrobe()
                            onStatusChange()
                            call.respond(buildJsonObject { 
                                put("success", true)
                                put("message", "Strobe flashlight started") 
                            })
                        }

                        // 6. POST /command/flash/stop  强制熄灭手电
                        post("/command/flash/stop") {
                            flashMode = "off"
                            flashlightController.stopAll()
                            onStatusChange()
                            call.respond(buildJsonObject { 
                                put("success", true)
                                put("message", "Flashlight stopped") 
                            })
                        }

                        // 7. POST /command/stop-all  熄灭灯光并停响警报
                        post("/command/stop-all") {
                            isRingActive = false
                            flashMode = "off"
                            ringController.stopRing()
                            flashlightController.stopAll()
                            onStatusChange()
                            call.respond(buildJsonObject { 
                                put("success", true)
                                put("message", "All hardware alerts stopped") 
                            })
                        }
                    }
                }
                server?.start(wait = false)
                Log.d("HttpServerManager", "Ktor server listening on port \${getPort()}")
            } catch (e: Exception) {
                Log.e("HttpServerManager", "Error starting Ktor server", e)
            }
        }
    }

    /**
     * 关闭服务释放资源
     */
    @Synchronized
    fun stop() {
        isRingActive = false
        flashMode = "off"
        ringController.stopRing()
        flashlightController.stopAll()
        
        scope.launch {
            try {
                server?.let {
                    it.stop(1, 3, TimeUnit.SECONDS)
                    Log.d("HttpServerManager", "Ktor server successfully shut down")
                }
                server = null
            } catch (e: Exception) {
                Log.e("HttpServerManager", "Error during Ktor shutdown cleanup", e)
            }
        }
    }
}`
  },
  {
    name: "RingController.kt",
    path: "/android/app/src/main/java/com/example/localfind/hardware/RingController.kt",
    role: "蜂鸣警报逻辑：采用双向阻断，指定 AudioAttributes.USAGE_ALARM 通道强制忽略勿扰模式，循环开启动画警笛声并提供安全释放接口。",
    lang: "kotlin",
    code: `package com.example.localfind.hardware

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log

class RingController(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    /**
     * 播放警报铃声，循环播放
     */
    @Synchronized
    fun startRing() {
        if (mediaPlayer?.isPlaying == true) return
        
        try {
            stopRing()
            // 依次尝试获取系统默认铃声、闹钟声、通知提示音
            val alert: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) 
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            if (alert == null) {
                Log.e("RingController", "No default system sound found")
                return
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alert)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM) // 采用 Alarm 通道，忽略静音模式，强制响铃
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            Log.d("RingController", "Started alarm ringtone successfully")
        } catch (e: Exception) {
            Log.e("RingController", "Failed to start ringtone", e)
        }
    }

    /**
     * 停止响铃并释放资源
     */
    @Synchronized
    fun stopRing() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
            Log.d("RingController", "Stopped alarm ringtone")
        } catch (e: Exception) {
            Log.e("RingController", "Error stopping ringtone", e)
        }
    }
}`
  },
  {
    name: "FlashlightController.kt",
    path: "/android/app/src/main/java/com/example/localfind/hardware/FlashlightController.kt",
    role: "相机闪光逻辑：调用 CameraManager 并检测后置闪光，以 Coroutines 协程轮询挂起（delay 200ms）轮空，保证不抢占、阻塞系统并发线程。",
    lang: "kotlin",
    code: `package com.example.localfind.hardware

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FlashlightController(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null
    
    private val scope = CoroutineScope(Dispatchers.Default)
    private var strobeJob: Job? = null
    private var isSteadyOn = false

    init {
        try {
            // 获取搭载了 LED 闪光灯的后置或前置摄像头
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash) {
                    cameraId = id
                    break
                }
            }
            if (cameraId == null && cameraManager.cameraIdList.isNotEmpty()) {
                cameraId = cameraManager.cameraIdList[0]
            }
        } catch (e: Exception) {
            Log.e("FlashlightController", "Failed to initialize CameraManager info", e)
        }
    }

    /**
     * 开启手电筒常亮
     */
    @Synchronized
    fun startSteady() {
        stopAll()
        val id = cameraId ?: return
        try {
            cameraManager.setTorchMode(id, true)
            isSteadyOn = true
            Log.d("FlashlightController", "Started steady flashlight")
        } catch (e: Exception) {
            Log.e("FlashlightController", "Failed to set torch mode to true", e)
        }
    }

    /**
     * 开启手电筒闪烁模式 (频率 200ms)
     * 使用协程控制，并在 job 取消时确保手电筒还原关闭
     */
    @Synchronized
    fun startStrobe() {
        stopAll()
        val id = cameraId ?: return
        strobeJob = scope.launch {
            var isOn = false
            try {
                while (isActive) {
                    isOn = !isOn
                    cameraManager.setTorchMode(id, isOn)
                    delay(200)
                }
            } catch (e: Exception) {
                Log.e("FlashlightController", "Strobe loop exception", e)
            } finally {
                // 确保协程取消或终止时自动关闭
                try {
                    cameraManager.setTorchMode(id, false)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        Log.d("FlashlightController", "Started strobe flashlight (200ms interval)")
    }

    /**
     * 停止全部动作 (关闭手电筒、取消闪烁协程)
     */
    @Synchronized
    fun stopAll() {
        strobeJob?.cancel()
        strobeJob = null
        
        val id = cameraId ?: return
        try {
            cameraManager.setTorchMode(id, false)
            isSteadyOn = false
            Log.d("FlashlightController", "Stopped both strobe and steady flashlight modes")
        } catch (e: Exception) {
            Log.e("FlashlightController", "Failed to force set torch mode to false", e)
        }
    }
}`
  },
  {
    name: "NetworkUtil.kt",
    path: "/android/app/src/main/java/com/example/localfind/util/NetworkUtil.kt",
    role: "网卡IP嗅探：过滤有线/无线底层网络节点，剔除 localhost 环路，拉取可用 IPv4，显示在首页便于微终端接入。",
    lang: "kotlin",
    code: `package com.example.localfind.util

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtil {
    /**
     * 获取当前手机局域网中的 IP 地址 (IPv4)
     * 过滤正在正常工作且非 Localhost 回环的 Wi-Fi 实网卡 IP
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}`
  },
  {
    name: "app: build.gradle.kts",
    path: "/android/app/build.gradle.kts",
    role: "模块构建：指定 Ktor Server 引擎、Netty 通信、Kotlin Serialization 编解码及 Compose 环境的打包设置。",
    lang: "kotlin",
    code: `plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.localfind"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.localfind"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // Ktor Server 依赖
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    
    // 协程
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}`
  },
  {
    name: "project: build.gradle.kts",
    path: "/android/build.gradle.kts",
    role: "项目工程：定义全局构建加载和 Gradle 插件注入机制。",
    lang: "kotlin",
    code: `plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}`
  },
  {
    name: "settings.gradle.kts",
    path: "/android/settings.gradle.kts",
    role: "工程依赖源设置：配置 Google 官方 Maven 仓储与中枢仓，包含 App 模块装载目录。",
    lang: "kotlin",
    code: `pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Local Find"
include(":app")`
  },
  {
    name: "libs.versions.toml",
    path: "/android/gradle/libs.versions.toml",
    role: "版本归总：聚合声明依赖物主属性、AGP、Kotlin、Ktor 服务器套件及 Compose UI BOM 库的版本控制清单。",
    lang: "toml",
    code: `[versions]
agp = "8.2.2"
kotlin = "1.9.22"
coreKtx = "1.12.0"
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"
lifecycleRuntimeKtx = "2.7.0"
activityCompose = "1.8.2"
composeBom = "2023.10.01"
ktor = "2.3.8"
coroutines = "1.8.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }

# Ktor Server Libraries
ktor-server-core = { group = "io.ktor", name = "ktor-server-core-jvm", version.ref = "ktor" }
ktor-server-netty = { group = "io.ktor", name = "ktor-server-netty-jvm", version.ref = "ktor" }
ktor-server-content-negotiation = { group = "io.ktor", name = "ktor-server-content-negotiation-jvm", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json-jvm", version.ref = "ktor" }

# Coroutines
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }`
  }
];


const MOCK_IP = "192.168.1.108";
const MOCK_PORT = 8888;

export default function App() {
  const [activeTab, setActiveTab] = useState<"simulator" | "code">("simulator");
  const [selectedFileIdx, setSelectedFileIdx] = useState(1); // Default to MainActivity.kt
  const [copiedIdx, setCopiedIdx] = useState<number | null>(null);

  // Simulated Device States
  const [isServiceRunning, setIsServiceRunning] = useState(true);
  const [ringActive, setRingActive] = useState(false);
  const [flashMode, setFlashMode] = useState<"off" | "steady" | "strobe">("off");
  
  // Strobe effect tracking
  const [isStrobeLightOn, setIsStrobeLightOn] = useState(false);
  const strobeIntervalRef = useRef<any>(null);

  // Playback sound beep interval
  const beepIntervalRef = useRef<any>(null);

  // Terminal history
  const [terminalLogs, setTerminalLogs] = useState<Array<{ id: number; input: string; output: string; time: string }>>([
    {
      id: 1,
      input: "System Initialized",
      output: "Checking Wi-Fi nodes... Found interface wlan0 with IP: 192.168.1.108.\nHttpServer listening on 0.0.0.0:8888.",
      time: new Date().toLocaleTimeString()
    }
  ]);

  // Audio trigger
  const playWebBeep = () => {
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = "sine";
      osc.frequency.setValueAtTime(950, audioCtx.currentTime);
      gain.gain.setValueAtTime(0.2, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.45);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.5);
    } catch (e) {
      console.warn("Audio Context blocked or unsupported. Interact first.", e);
    }
  };

  // Sound loop manager
  useEffect(() => {
    if (ringActive) {
      playWebBeep();
      beepIntervalRef.current = setInterval(playWebBeep, 800);
    } else {
      if (beepIntervalRef.current) {
        clearInterval(beepIntervalRef.current);
        beepIntervalRef.current = null;
      }
    }
    return () => {
      if (beepIntervalRef.current) clearInterval(beepIntervalRef.current);
    };
  }, [ringActive]);

  // Strobe flash timing manager
  useEffect(() => {
    if (flashMode === "strobe") {
      setIsStrobeLightOn(true);
      strobeIntervalRef.current = setInterval(() => {
        setIsStrobeLightOn((prev) => !prev);
      }, 200);
    } else if (flashMode === "steady") {
      setIsStrobeLightOn(true);
      if (strobeIntervalRef.current) {
        clearInterval(strobeIntervalRef.current);
        strobeIntervalRef.current = null;
      }
    } else {
      setIsStrobeLightOn(false);
      if (strobeIntervalRef.current) {
        clearInterval(strobeIntervalRef.current);
        strobeIntervalRef.current = null;
      }
    }

    return () => {
      if (strobeIntervalRef.current) clearInterval(strobeIntervalRef.current);
    };
  }, [flashMode]);

  // Trigger terminal commands
  const executeMockCommand = (apiEndpoint: string, method: "GET" | "POST") => {
    const timestamp = new Date().toLocaleTimeString();
    const curlCommand = `curl -X ${method} http://${MOCK_IP}:${MOCK_PORT}${apiEndpoint}`;
    
    let mockResponse = "";
    
    if (!isServiceRunning && apiEndpoint !== "/status") {
      mockResponse = `curl: (7) Failed to connect to ${MOCK_IP} port ${MOCK_PORT}: Connection refused\n[提示] 手机寻机前台服务未启动或已关闭，Ktor 服务器不在线。`;
    } else {
      switch (apiEndpoint) {
        case "/status":
          mockResponse = JSON.stringify({
            service: isServiceRunning ? "running" : "stopped",
            ring_active: ringActive,
            flash_mode: flashMode
          }, null, 2);
          break;
        case "/command/ring/start":
          setRingActive(true);
          mockResponse = JSON.stringify({ success: true, message: "Ring started" }, null, 2);
          break;
        case "/command/ring/stop":
          setRingActive(false);
          mockResponse = JSON.stringify({ success: true, message: "Ring stopped" }, null, 2);
          break;
        case "/command/flash/steady/start":
          setFlashMode("steady");
          mockResponse = JSON.stringify({ success: true, message: "Steady flashlight started" }, null, 2);
          break;
        case "/command/flash/strobe/start":
          setFlashMode("strobe");
          mockResponse = JSON.stringify({ success: true, message: "Strobe flashlight started" }, null, 2);
          break;
        case "/command/flash/stop":
          setFlashMode("off");
          mockResponse = JSON.stringify({ success: true, message: "Flashlight stopped" }, null, 2);
          break;
        case "/command/stop-all":
          setRingActive(false);
          setFlashMode("off");
          mockResponse = JSON.stringify({ success: true, message: "All hardware alerts stopped" }, null, 2);
          break;
        default:
          mockResponse = `404 Not Found`;
      }
    }

    setTerminalLogs((prev) => [
      ...prev,
      {
        id: Date.now(),
        input: curlCommand,
        output: mockResponse,
        time: timestamp
      }
    ]);
  };

  const copyToClipboard = (text: string, index: number) => {
    navigator.clipboard.writeText(text);
    setCopiedIdx(index);
    setTimeout(() => {
      setCopiedIdx(null);
    }, 2000);
  };

  const clearLogs = () => {
    setTerminalLogs([]);
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 flex flex-col font-sans selection:bg-indigo-600 selection:text-white">
      
      {/* Dynamic Header */}
      <header className="border-b border-slate-200 bg-white sticky top-0 z-30 px-6 py-4 shadow-sm">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-indigo-50 border border-indigo-100 text-indigo-600">
              <Radio className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-xl font-extrabold tracking-tight text-slate-850">Local Find <span className="text-indigo-600 font-normal text-xs font-sans">v1.0</span></h1>
                <span className="px-2.5 py-0.5 text-[10px] font-mono font-bold rounded-full bg-indigo-50 text-indigo-600 border border-indigo-100 uppercase tracking-wider">
                  MVP-A Phase Finished
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-0.5">同一 Wi-Fi 连接下局域网电脑 HTTP 调用控制端，声光报警，完全不依赖任何第三方云网络服务</p>
            </div>
          </div>
          
          {/* Tab Button Toggles */}
          <div className="flex bg-slate-100 p-1 rounded-2xl border border-slate-200/60">
            <button
              onClick={() => setActiveTab("simulator")}
              className={`flex items-center gap-2 px-4 py-2 text-xs font-semibold rounded-xl transition-all ${
                activeTab === "simulator"
                  ? "bg-white text-indigo-600 font-bold shadow-sm border border-slate-200"
                  : "text-slate-500 hover:text-slate-800"
              }`}
            >
              <Smartphone className="w-4 h-4" />
              虚拟联调仿真器
            </button>
            <button
              onClick={() => setActiveTab("code")}
              className={`flex items-center gap-2 px-4 py-2 text-xs font-semibold rounded-xl transition-all ${
                activeTab === "code"
                  ? "bg-white text-indigo-600 font-bold shadow-sm border border-slate-200"
                  : "text-slate-500 hover:text-slate-800"
              }`}
            >
              <Code2 className="w-4 h-4" />
              Android 完整源码包
            </button>
          </div>
        </div>
      </header>

      {/* Main Container Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 md:p-6 flex flex-col gap-6">

        {activeTab === "simulator" ? (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch">
            
            {/* Left Col: Device Simulator (5 cols) */}
            <div className="lg:col-span-5 flex flex-col items-center justify-center">
              
              {/* LED Flash Indicator on top */}
              <div className="mb-4 flex flex-col items-center gap-1.5">
                <span className="text-[10px] font-mono uppercase tracking-widest text-slate-400 font-bold">Camera Torch LED Simulator</span>
                <div className="flex items-center gap-2.5">
                  <div className={`w-8 h-8 rounded-full border-2 transition-all duration-100 flex items-center justify-center ${
                    isStrobeLightOn 
                      ? "bg-yellow-400 border-yellow-300 shadow-[0_0_25px_15px_rgba(251,191,36,0.55)]" 
                      : "bg-slate-200 border-slate-350 shadow-inner"
                  }`}>
                    <Zap className={`w-4 h-4 ${isStrobeLightOn ? "text-slate-900" : "text-slate-400"}`} />
                  </div>
                  <span className="text-xs font-mono font-bold text-slate-500">
                    状态: {flashMode === "steady" ? "常亮常通" : flashMode === "strobe" ? "爆闪 Strobe (200ms)" : "关闭"}
                  </span>
                </div>
              </div>

              {/* iPhone Frame */}
              <div className="relative w-[310px] h-[640px] bg-slate-800 rounded-[48px] p-3.5 shadow-2xl border-[5px] border-slate-700 flex flex-col overflow-hidden">
                
                {/* Speaker Line earcap */}
                <div className="absolute top-1 left-1/2 transform -translate-x-1/2 w-32 h-4.5 bg-slate-800 rounded-b-xl z-20 flex items-center justify-center">
                  <div className="w-12 h-1 bg-slate-700 rounded-full"></div>
                </div>

                {/* Simulated Screen Inner */}
                <div className="relative flex-1 bg-slate-50 rounded-[34px] flex flex-col p-4 select-none overflow-y-auto no-scrollbar justify-between text-slate-900">
                  
                  {/* Status Bar */}
                  <div className="flex items-center justify-between text-[11px] font-mono text-slate-500 px-2 pt-1 font-semibold">
                    <span>9:41 AM</span>
                    <div className="flex items-center gap-1.5">
                      <Wifi className="w-3 h-3 text-indigo-600" />
                      <span className="text-indigo-600 font-bold">LAN</span>
                      <span>100%</span>
                    </div>
                  </div>

                  {/* Android Card Header */}
                  <div className="mt-4 px-2">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold text-indigo-600 tracking-wide uppercase">Local Find Terminal</span>
                      <Smartphone className="w-4 h-4 text-indigo-600" />
                    </div>
                    <h2 className="text-lg font-black text-slate-850 mt-1 border-b border-slate-200 pb-2">Local Find 被寻找端</h2>
                  </div>

                  {/* Notification Bar inside phone */}
                  {isServiceRunning && (
                    <div className="mx-1 mt-2.5 bg-indigo-900 border border-indigo-950 p-3 rounded-2xl flex items-start gap-2.5 shadow-md text-white">
                      <div className="p-1 rounded bg-white/10 text-white mt-0.5">
                        <Server className="w-3.5 h-3.5 animate-pulse" />
                      </div>
                      <div className="flex-1">
                        <h4 className="text-[10px] font-bold leading-tight">Local Find 寻机服务活跃</h4>
                        <p className="text-[9px] text-indigo-100 opacity-90 leading-normal mt-0.5">常驻前台运行中，持续监听内网指令</p>
                      </div>
                    </div>
                  )}

                  {/* Simulator Screen Body (Jetpack Compose View replica) */}
                  <div className="flex-1 flex flex-col justify-center gap-4 py-4">
                    
                    {/* Status Board */}
                    <div className="bg-white border border-slate-200 rounded-3xl p-4 flex flex-col gap-2 shadow-xs">
                      <span className="text-[11px] text-slate-400 font-bold tracking-widest uppercase text-center block w-full">监测状态</span>
                      
                      <div className="flex items-center justify-center gap-2 py-1">
                        <span className={`w-2.5 h-2.5 rounded-full ${isServiceRunning ? "bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.8)] animate-ping" : "bg-red-500"}`}></span>
                        <span className={`text-xs font-black uppercase tracking-wider ${isServiceRunning ? "text-emerald-600" : "text-red-500"}`}>
                          {isServiceRunning ? "SERVICE: RUNNING" : "SERVICE: STOPPED"}
                        </span>
                      </div>

                      <div className="border-t border-slate-100 my-2 pt-2 text-[11px] flex flex-col gap-1 text-slate-600">
                        <div className="flex justify-between font-mono">
                          <span>当前 IP:</span>
                          <span className={isServiceRunning ? "text-indigo-600 font-bold" : "text-slate-400"}>
                            {isServiceRunning ? MOCK_IP : "未连接"}
                          </span>
                        </div>
                        <div className="flex justify-between font-mono">
                          <span>监听端口:</span>
                          <span className="text-slate-700 font-bold">{MOCK_PORT}</span>
                        </div>
                      </div>

                      {isServiceRunning && (
                        <div className="bg-slate-50 p-1.5 rounded-xl text-[9px] font-mono text-center text-indigo-600 font-bold border border-slate-100 truncate">
                          http://{MOCK_IP}:{MOCK_PORT}
                        </div>
                      )}
                    </div>

                    {/* Controller Action Indicators - Geometric styles */}
                    <div className="grid grid-cols-2 gap-2">
                      <div className={`p-2.5 rounded-2xl border text-center transition-all ${
                        ringActive 
                          ? "bg-indigo-50 border-indigo-200 text-indigo-700" 
                          : "bg-white border-slate-200 text-slate-400"
                      }`}>
                        <span className="text-[9.5px] font-bold block uppercase tracking-wider opacity-60">鸣报器</span>
                        <div className="flex items-center justify-center gap-1 mt-0.5">
                          <Volume2 className={`w-3.5 h-3.5 ${ringActive ? "animate-bounce text-indigo-600" : "text-slate-300"}`} />
                          <span className="text-xs font-bold">{ringActive ? "长鸣呼叫" : "静音关"}</span>
                        </div>
                      </div>

                      <div className={`p-2.5 rounded-2xl border text-center transition-all ${
                        flashMode !== "off" 
                          ? "bg-yellow-50 border-yellow-250 text-yellow-700" 
                          : "bg-white border-slate-200 text-slate-400"
                      }`}>
                        <span className="text-[9.5px] font-bold block uppercase tracking-wider opacity-60">闪光模块</span>
                        <div className="flex items-center justify-center gap-1 mt-0.5">
                          <Zap className={`w-3.5 h-3.5 ${flashMode !== "off" ? "text-yellow-500" : "text-slate-300"}`} />
                          <span className="text-xs font-bold">
                            {flashMode === "steady" ? "手电常亮" : flashMode === "strobe" ? "极速闪爆" : "关闭"}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Simulation Triggers (Buttons replica) */}
                    <div className="flex flex-col gap-1.5 mt-1">
                      <div className="grid grid-cols-2 gap-1.5">
                        <button
                          onClick={() => setIsServiceRunning(true)}
                          disabled={isServiceRunning}
                          className="py-2.5 px-3 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-40 rounded-2xl text-[11px] font-bold text-white transition-all shadow-xs active:scale-95 cursor-pointer"
                        >
                          启动寻机服务
                        </button>
                        <button
                          onClick={() => {
                            setIsServiceRunning(false);
                            setRingActive(false);
                            setFlashMode("off");
                          }}
                          disabled={!isServiceRunning}
                          className="py-2.5 px-3 bg-slate-800 hover:bg-slate-900 disabled:opacity-40 rounded-2xl text-[11px] font-bold text-white transition-all shadow-xs active:scale-95 cursor-pointer"
                        >
                          停止寻机服务
                        </button>
                      </div>

                      {/* Mock Hardware Test triggers */}
                      <span className="text-[9px] text-slate-400 text-center uppercase tracking-widest font-black my-1">本地动作调试</span>
                      
                      <div className="grid grid-cols-2 gap-1.5">
                        <button
                          onClick={() => {
                            if (!isServiceRunning) return;
                            setRingActive(!ringActive);
                          }}
                          disabled={!isServiceRunning}
                          className="py-2 px-2.5 bg-white hover:bg-slate-100 disabled:opacity-35 border border-slate-200 text-slate-700 rounded-xl text-[10.5px] font-bold transition-all active:scale-95 cursor-pointer"
                        >
                          {ringActive ? "停止响铃" : "测试响铃"}
                        </button>
                        <button
                          onClick={() => {
                            if (!isServiceRunning) return;
                            setFlashMode("steady");
                          }}
                          disabled={!isServiceRunning}
                          className="py-2 px-2.5 bg-white hover:bg-slate-100 disabled:opacity-35 border border-slate-200 text-slate-700 rounded-xl text-[10.5px] font-bold transition-all active:scale-95 cursor-pointer"
                        >
                          测试常亮
                        </button>
                      </div>

                      <div className="grid grid-cols-2 gap-1.5">
                        <button
                          onClick={() => {
                            if (!isServiceRunning) return;
                            setFlashMode("strobe");
                          }}
                          disabled={!isServiceRunning}
                          className="py-2 px-2.5 bg-white hover:bg-slate-100 disabled:opacity-35 border border-slate-200 text-slate-700 rounded-xl text-[10.5px] font-bold transition-all active:scale-95 cursor-pointer"
                        >
                          测试爆闪
                        </button>
                        <button
                          onClick={() => {
                            if (!isServiceRunning) return;
                            setFlashMode("off");
                          }}
                          disabled={!isServiceRunning}
                          className="py-2 px-2.5 bg-white hover:bg-slate-100 disabled:opacity-35 border border-slate-200 text-slate-700 rounded-xl text-[10.5px] font-bold transition-all active:scale-95 cursor-pointer"
                        >
                          关闭手电
                        </button>
                      </div>

                      <button
                        onClick={() => {
                          setRingActive(false);
                          setFlashMode("off");
                        }}
                        disabled={!isServiceRunning}
                        className="py-2.5 px-3 bg-rose-50 text-rose-600 hover:bg-rose-100 disabled:opacity-20 border border-rose-100 rounded-2xl text-[11px] font-extrabold transition-all active:scale-95 mt-1 cursor-pointer"
                      >
                        一键停止全部动作
                      </button>
                    </div>

                  </div>

                  {/* Android Bottom Bar Navigation */}
                  <div className="flex items-center justify-around py-1 border-t border-slate-200/60 mt-auto">
                    <span className="w-3.5 h-3.5 border-2 border-slate-400 rounded-xs"></span>
                    <span className="w-4 h-4 border-2 border-slate-400 rounded-full"></span>
                    <span className="w-4 h-4 text-slate-400 font-bold">&#x25C0;</span>
                  </div>

                </div>

              </div>
            </div>

            {/* Right Col: API & Control Panel (7 cols) - Styled light slate, border-slate-200 */}
            <div className="lg:col-span-7 flex flex-col gap-6">
              
              {/* API Action Control Board */}
              <div className="bg-white border border-slate-200 rounded-3xl p-6 shadow-sm flex flex-col gap-4 text-slate-800">
                <div className="flex items-center gap-2">
                  <Terminal className="w-5 h-5 text-indigo-600" />
                  <h3 className="text-sm uppercase tracking-wider font-bold text-slate-800">电脑端 API 调用联调模拟区</h3>
                </div>
                <p className="text-xs text-slate-500 leading-normal font-medium">
                  假设连接在同一个 Wi-Fi 网络内，你的电脑可以直接在命令行向手机内置的 Ktor 服务下发 HTTP 请求。点击右侧测试按钮，即可瞬间触发左侧手机模拟端的状态：
                </p>

                {/* API Action List */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  
                  {/* Status Check card */}
                  <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 flex flex-col justify-between gap-3">
                    <div>
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] uppercase font-mono bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded-md border border-emerald-200 font-bold">GET</span>
                        <span className="text-[11.5px] font-mono text-indigo-600 font-bold">/status</span>
                      </div>
                      <p className="text-[10.5px] text-slate-500 mt-1 lines-normal">查询当前手机活跃运行态和声光硬件开关</p>
                    </div>
                    <button
                      onClick={() => executeMockCommand("/status", "GET")}
                      className="w-full py-2 bg-white hover:bg-slate-100 text-xs font-bold rounded-xl text-indigo-600 transition-all border border-slate-200 shadow-3xs cursor-pointer hover:border-indigo-500"
                    >
                      发送请求 Status
                    </button>
                  </div>

                  {/* Ring Start Card */}
                  <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 flex flex-col justify-between gap-3">
                    <div>
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] uppercase font-mono bg-amber-100 text-amber-850 px-2 py-0.5 rounded-md border border-amber-250 font-bold">POST</span>
                        <span className="text-[11.5px] font-mono text-indigo-600 font-bold leading-none">/command/ring/start</span>
                      </div>
                      <p className="text-[10.5px] text-slate-500 mt-1 leading-normal">开启循环警笛呼叫，响亮鸣空</p>
                    </div>
                    <button
                      onClick={() => executeMockCommand("/command/ring/start", "POST")}
                      className="w-full py-2 bg-white hover:bg-slate-100 text-xs font-bold rounded-xl text-amber-600 transition-all border border-slate-200 shadow-3xs cursor-pointer hover:border-amber-500"
                    >
                      拉响手机警报
                    </button>
                  </div>

                  {/* Flash Steady Card */}
                  <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 flex flex-col justify-between gap-3">
                    <div>
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] uppercase font-mono bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-md border border-indigo-100 font-bold">POST</span>
                        <span className="text-[11.5px] font-mono text-indigo-600 font-bold">/command/flash/steady/...</span>
                      </div>
                      <p className="text-[10.5px] text-slate-500 mt-1 leading-normal">开启手电筒常亮，黑夜环境下极易找到</p>
                    </div>
                    <button
                      onClick={() => executeMockCommand("/command/flash/steady/start", "POST")}
                      className="w-full py-2 bg-white hover:bg-slate-100 text-xs font-bold rounded-xl text-indigo-600 transition-all border border-slate-200 shadow-3xs cursor-pointer hover:border-indigo-500"
                    >
                      常亮手电筒
                    </button>
                  </div>

                  {/* Flash Strobe Card */}
                  <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 flex flex-col justify-between gap-3">
                    <div>
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] uppercase font-mono bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-md border border-indigo-100 font-bold">POST</span>
                        <span className="text-[11.5px] font-mono text-indigo-600 font-bold">/command/flash/strobe/...</span>
                      </div>
                      <p className="text-[10.5px] text-slate-500 mt-1 leading-normal">开启 200ms 的高速频乘爆闪闪烁</p>
                    </div>
                    <button
                      onClick={() => executeMockCommand("/command/flash/strobe/start", "POST")}
                      className="w-full py-2 bg-white hover:bg-slate-100 text-xs font-bold rounded-xl text-indigo-600 transition-all border border-slate-200 shadow-3xs cursor-pointer hover:border-indigo-500"
                    >
                      闪烁爆闪寻机
                    </button>
                  </div>

                  {/* Stop Tone or Stop All Card */}
                  <div className="p-4 rounded-2xl bg-rose-50 border border-rose-250 flex flex-col justify-between gap-3 md:col-span-2 md:flex-row md:items-center">
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] uppercase font-mono bg-rose-100 text-rose-800 px-2 py-0.5 rounded-md border border-rose-200 font-bold">POST</span>
                        <span className="text-[11.5px] font-mono text-rose-800 font-bold">/command/stop-all</span>
                      </div>
                      <p className="text-[10.5px] text-rose-950/60 mt-1">一键指令同时下线声音和闪烁，寻机完毕后立刻使手机归于安静。</p>
                    </div>
                    <button
                      onClick={() => executeMockCommand("/command/stop-all", "POST")}
                      className="px-6 py-2.5 bg-rose-600 hover:bg-rose-700 text-white text-xs font-extrabold rounded-xl shadow-xs transition-all cursor-pointer shadow-rose-200"
                    >
                      一键停止全部
                    </button>
                  </div>

                </div>

              </div>

              {/* Console logs - Geometric wrapper (black screen console within light card layout) */}
              <div className="bg-slate-900 rounded-3xl border border-slate-950 overflow-hidden flex flex-col flex-1 min-h-[220px] shadow-sm">
                <div className="px-5 py-3.5 border-b border-slate-800 bg-slate-950 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Terminal className="w-4.5 h-4.5 text-slate-400" />
                    <span className="text-xs font-mono font-semibold text-slate-300">HTTP REST Simulator Logs</span>
                  </div>
                  <button
                    onClick={clearLogs}
                    className="text-[10.5px] text-slate-500 hover:text-slate-300 flex items-center gap-1 transition-all cursor-pointer"
                  >
                    <RefreshCw className="w-3 h-3" />
                    重置审计流
                  </button>
                </div>

                <div className="p-4 flex-1 font-mono text-[11px] overflow-y-auto max-h-[300px] bg-slate-950 flex flex-col gap-3">
                  {terminalLogs.length === 0 ? (
                    <div className="text-slate-600 italic text-center py-6">暂无任何呼唤调试指令</div>
                  ) : (
                    terminalLogs.map((log) => (
                      <div key={log.id} className="border-l-2 border-indigo-500 pl-3.5 py-0.5">
                        <div className="flex items-center justify-between text-slate-500 text-[10px] mb-1">
                          <span>指令下发时间: {log.time}</span>
                        </div>
                        <div className="text-indigo-400 font-bold mb-1">{log.input}</div>
                        <pre className="text-slate-400 text-[10px] bg-slate-900/60 p-2.5 rounded border border-slate-800/40 whitespace-pre-wrap leading-relaxed">{log.output}</pre>
                      </div>
                    ))
                  )}
                </div>
              </div>

            </div>

          </div>
        ) : (
          /* File Explorer & Code tab */
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch">
            
            {/* Sidebar selection (4 cols) */}
            <div className="lg:col-span-4 bg-white border border-slate-200 rounded-3xl p-5 flex flex-col gap-4 shadow-sm">
              <div className="flex items-center gap-2.5">
                <FolderOpen className="w-5 h-5 text-indigo-600" />
                <h3 className="text-sm font-bold text-slate-800 uppercase tracking-wide">MVP-A 寻机端工程结构</h3>
              </div>
              <p className="text-xs text-slate-500 leading-relaxed font-medium">
                项目采用模块分离的清晰设计。请在下方点击选择文件，并在右侧查看详细代码说明与完整文件流：
              </p>

              {/* Package Folder Tree inside side */}
              <div className="flex flex-col gap-2 flex-1 overflow-y-auto min-h-[300px] lg:max-h-[580px] pr-1">
                
                {ANDROID_FILES.map((file, idx) => {
                  const isCur = selectedFileIdx === idx;
                  const isGradle = file.name.endsWith("gradle.kts") || file.name.endsWith(".toml");
                  const isManifest = file.name === "AndroidManifest.xml";
                  return (
                    <button
                      key={file.name}
                      onClick={() => setSelectedFileIdx(idx)}
                      className={`w-full p-3 rounded-2xl text-left transition-all border ${
                        isCur 
                          ? "bg-indigo-50/70 border-indigo-200 text-indigo-700 shadow-3xs" 
                          : "bg-slate-50/65 border-transparent text-slate-600 hover:bg-slate-50 hover:text-slate-800"
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <FileCode className={`w-4 h-4 ${isCur ? "text-indigo-600" : isGradle ? "text-slate-400" : isManifest ? "text-purple-500" : "text-slate-500"}`} />
                        <span className="text-xs font-bold font-mono truncate">{file.name}</span>
                      </div>
                      <p className="text-[10px] text-slate-500 mt-1 line-clamp-2 leading-relaxed font-sans">{file.role}</p>
                      <span className="text-[9px] font-mono block text-slate-400 mt-0.5 truncate">{file.path}</span>
                    </button>
                  );
                })}

              </div>
            </div>

            {/* Code Body (8 cols) */}
            <div className="lg:col-span-8 flex flex-col bg-white border border-slate-200 rounded-3xl overflow-hidden shadow-sm">
              
              {/* Toolbar */}
              <div className="px-5 py-4 bg-slate-50 border-b border-slate-200 flex flex-col md:flex-row md:items-center justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 text-[9.5px] font-mono bg-indigo-100 text-indigo-700 rounded-md border border-indigo-200 font-bold uppercase tracking-wider">
                      {ANDROID_FILES[selectedFileIdx].lang}
                    </span>
                    <h4 className="text-xs font-mono font-bold text-slate-800">{ANDROID_FILES[selectedFileIdx].name}</h4>
                  </div>
                  <p className="text-[11px] text-slate-400 mt-0.5 font-mono font-medium">{ANDROID_FILES[selectedFileIdx].path}</p>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => copyToClipboard(ANDROID_FILES[selectedFileIdx].code, selectedFileIdx)}
                    className="flex items-center gap-1.5 px-3 py-1.5 bg-white hover:bg-slate-100 border border-slate-200 rounded-xl text-xs text-slate-700 hover:text-indigo-600 font-bold transition-all shadow-3xs cursor-pointer"
                  >
                    {copiedIdx === selectedFileIdx ? (
                      <>
                        <Check className="w-3.5 h-3.5 text-indigo-600" />
                        <span className="text-indigo-600 font-bold">复制成功</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3.5 h-3.5 text-slate-500" />
                        <span>一键复制代码</span>
                      </>
                    )}
                  </button>
                </div>
              </div>

              {/* Code Panel Display */}
              <div className="flex-1 p-5 font-mono text-xs overflow-auto bg-slate-950/95 leading-relaxed min-h-[400px] lg:max-h-[580px] shadow-inner text-slate-300">
                <pre className="select-text whitespace-pre overflow-x-auto tab-size-4">
                  <code>{ANDROID_FILES[selectedFileIdx].code}</code>
                </pre>
              </div>

              {/* Status Note Footer */}
              <div className="bg-indigo-50/40 border-t border-indigo-100/60 p-4 flex gap-3 items-start">
                <Info className="w-4.5 h-4.5 text-indigo-600 mt-0.5 flex-shrink-0" />
                <p className="text-[11.5px] text-slate-600 leading-relaxed font-medium">
                  本源码完全在 <span className="text-indigo-700 font-semibold font-mono">/android/*</span> 实际物理目录结构中进行了存储。你可以随时点击 settings &gt; export 导出整套 Gradle-Kotlin 程序包，也支持直接在宿主机解压缩导入最新的 Android Studio 进行打包部署编译。
                </p>
              </div>

            </div>

          </div>
        )}

        {/* Global Developer Instruction Footboard */}
        <section className="bg-white rounded-3xl border border-slate-200 p-6 mt-4 flex flex-col gap-5 shadow-sm text-slate-800">
          <div className="flex items-center gap-2">
            <BookOpen className="w-5 h-5 text-indigo-600" />
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-800">Local Find 首阶段 MVP-A 技术验证与测试简述</h3>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 text-xs">
            
            <div className="p-5 bg-slate-50 border border-slate-200/50 rounded-2xl flex flex-col gap-2">
              <span className="font-bold text-indigo-600 flex items-center gap-1.5 uppercase tracking-wide">
                <span className="w-2 h-2 rounded-full bg-indigo-500"></span>
                第一步：导入与编译
              </span>
              <p className="text-slate-500 leading-relaxed font-medium mt-1">
                使用 Android Studio 导入项目中的 <code className="font-mono text-indigo-650 bg-indigo-50/50 px-1 py-0.5 rounded border border-indigo-100">/android</code> 目录。点击 Sync Gradle。项目已完全根据 Version Catalogs (toml) 进行配置，支持 Java 17 编译，对 Compose M3、Ktor 具有完整兼容。
              </p>
            </div>

            <div className="p-5 bg-slate-50 border border-slate-200/50 rounded-2xl flex flex-col gap-2">
              <span className="font-bold text-indigo-600 flex items-center gap-1.5 uppercase tracking-wide">
                <span className="w-2 h-2 rounded-full bg-indigo-500"></span>
                第二步：启动寻机服务
              </span>
              <p className="text-slate-500 leading-relaxed font-medium mt-1">
                在手机中安装并运行该 App。连接 Wi-Fi 并授予应用通知、相机组件权限。点击应用首页的「启动寻机服务」按钮。此时，系统将在通知栏拉起常驻前台守护，并打印当前的局域网 IP (例如 192.168.1.108:8888)。
              </p>
            </div>

            <div className="p-5 bg-slate-50 border border-slate-200/50 rounded-2xl flex flex-col gap-2">
              <span className="font-bold text-indigo-600 flex items-center gap-1.5 uppercase tracking-wide">
                <span className="w-2 h-2 rounded-full bg-indigo-500"></span>
                第三步：电脑命令呼叫
              </span>
              <p className="text-slate-500 leading-relaxed font-medium mt-1">
                确保你的电脑和手机连接在同一个局域网。直接启动电脑终端输入右下角的 cURL 指令。手机将立刻跳脱勿扰，发出最高响度报警音，同时背部闪光灯高速爆闪！
              </p>
            </div>

          </div>

          {/* Verification Code Box */}
          <div className="bg-indigo-900 text-white border border-indigo-950 p-6 rounded-3xl flex flex-col md:flex-row md:items-center justify-between gap-6 mt-1 shadow-sm">
            <div className="flex-1">
              <h4 className="text-sm font-bold text-white flex items-center gap-2 uppercase tracking-wide">
                <Terminal className="w-4.5 h-4.5 text-indigo-200" />
                终端 cURL 极速调试集 (支持响铃、闪光控制)
              </h4>
              <p className="text-[11.5px] text-indigo-200/70 mt-1 lines-normal">
                测试时，请将下方命令中 <code className="font-mono text-white bg-indigo-950 px-1.5 py-0.5 rounded border border-indigo-800">192.168.1.x</code> 替换为对应手机首页显示的局域网实际 IP 地址。
              </p>
            </div>

            <div className="flex flex-col gap-2 bg-indigo-950 border border-indigo-800/40 p-4 rounded-2xl font-mono text-[11px] text-indigo-300 select-text max-w-lg min-w-full md:min-w-[420px] overflow-x-auto whitespace-pre">
              <div className="text-indigo-400 font-bold"># 1. 触发手机报警爆闪：</div>
              <div className="text-slate-205 font-bold">curl -X POST http://192.168.1.x:8888/command/flash/strobe/start</div>
              <div className="mt-1 text-indigo-400 font-bold"># 2. 触发手机警声长鸣：</div>
              <div className="text-slate-205 font-bold">curl -X POST http://192.168.1.x:8888/command/ring/start</div>
              <div className="mt-1 text-indigo-400 font-bold"># 3. 极速停止全部报警（归于平静）：</div>
              <div className="text-slate-205 font-bold">curl -X POST http://192.168.1.x:8888/command/stop-all</div>
            </div>
          </div>

        </section>

      </main>

      {/* Humble Footer */}
      <footer className="border-t border-slate-200 bg-white py-6 text-center mt-auto text-[10.5px] font-bold text-slate-450 uppercase tracking-widest leading-loose">
        <div>&copy; 2026 Local Find Engineering Team. Crafted with Geometric Balance style.</div>
      </footer>

    </div>
  );
}
