package com.example.localfind

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import android.widget.Toast
import com.example.localfind.service.FindPhoneForegroundService
import com.example.localfind.server.NsdStatus
import com.example.localfind.server.ServerStatus
import com.example.localfind.server.NsdDiscoveryManager
import com.example.localfind.server.DiscoveryStatus
import com.example.localfind.server.DiscoveredDevice
import com.example.localfind.auth.RemoteDeviceTokenStore
import com.example.localfind.ui.MainScreen
import com.example.localfind.util.NetworkUtil

class MainActivity : FragmentActivity() {

    private var foregroundService: FindPhoneForegroundService? = null
    private var isServiceBound by mutableStateOf(false)

    // 反射给 Jetpack Compose 驱动的响应式核心状态
    private var ringActiveState by mutableStateOf(false)
    private var flashModeState by mutableStateOf("off")
    private var isServiceRunningState by mutableStateOf(false)
    private var serverStatusState by mutableStateOf(ServerStatus.STOPPED)
    private var lastServerErrorState by mutableStateOf<String?>(null)
    private var wakeLockHeldState by mutableStateOf(false)
    private var wifiLockHeldState by mutableStateOf(false)
    private var deviceIpState by mutableStateOf<String?>(null)
    private var nsdStatusState by mutableStateOf(NsdStatus.IDLE)
    private var nsdServiceTypeState by mutableStateOf("_localfind._tcp.")
    private var pairingTokenState by mutableStateOf("")

    // NSD Discovery 状态
    private lateinit var nsdDiscoveryManager: NsdDiscoveryManager
    private var discoveryStatusState by mutableStateOf(DiscoveryStatus.IDLE)
    private var discoveredDevicesState by mutableStateOf(listOf<DiscoveredDevice>())
    private lateinit var remoteTokenStore: RemoteDeviceTokenStore

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

            // 监听服务状态变动的回调，实现远程/外部触发后，App 界面可以同步瞬时改变
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
            serverStatusState = service.getServerStatus()
            lastServerErrorState = service.getLastServerError()
            wakeLockHeldState = service.isWakeLockHeld()
            wifiLockHeldState = service.isWifiLockHeld()
            nsdStatusState = service.getNsdStatus()
            nsdServiceTypeState = service.getNsdServiceType()
            pairingTokenState = service.getPairingToken()
            deviceIpState = service.getLocalIp()
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

        // 初始化扫描器
        nsdDiscoveryManager = NsdDiscoveryManager(
            context = this,
            onStatusChange = { discoveryStatusState = it },
            onDevicesUpdate = { discoveredDevicesState = it }
        )

        remoteTokenStore = RemoteDeviceTokenStore(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                        MainScreen(
                            isServiceRunning = isServiceRunningState,
                            serverStatus = serverStatusState,
                            lastServerError = lastServerErrorState,
                            wakeLockHeld = wakeLockHeldState,
                            wifiLockHeld = wifiLockHeldState,
                            localIp = deviceIpState,
                            port = 8888,
                            ringActive = ringActiveState,
                            flashMode = flashModeState,
                            nsdStatus = nsdStatusState,
                            nsdServiceType = nsdServiceTypeState,
                            discoveryStatus = discoveryStatusState,
                            discoveredDevices = discoveredDevicesState,
                            pairingToken = pairingTokenState,
                            remoteTokenStore = remoteTokenStore,
                            onStartService = { startAndBindService() },
                            onStopService = { shutdownService() },
                            onRestartServer = { foregroundService?.restartServer() },
                            onRegenerateToken = {
                                pairingTokenState = foregroundService?.regeneratePairingToken() ?: ""
                            },
                            onStartDiscovery = { nsdDiscoveryManager.startDiscovery() },
                            onStopDiscovery = { nsdDiscoveryManager.stopDiscovery() },
                            onOpenDevice = { device ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(device.controlUrl))
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                        onScanQrCode = {
                            checkAndRequestPermissions()
                        },
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
                        onRequestPermission = { checkAndRequestPermissions() },
                        onOpenBatterySettings = { openBatteryOptimizationSettings() },
                        onAuthenticate = { reason, onSuccess, onFailure ->
                            authenticateLocalUser(reason, onSuccess, onFailure)
                        }
                    )
                }
            }
        }

        checkAndRequestPermissions()
        refreshServiceStatus()
    }

    private fun authenticateLocalUser(
        reason: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        
        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(this)
                val biometricPrompt = BiometricPrompt(this, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            onFailure(errString.toString())
                        }

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            // This is for individual failed attempts (wrong finger), 
                            // error callback is called for permanent failure/cancel
                        }
                    })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("本地身份认证")
                    .setSubtitle(reason)
                    .setAllowedAuthenticators(authenticators)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onFailure("请先在本机系统中设置锁屏密码或生物识别")
            }
            else -> {
                onFailure("本机认证不可用或未设置锁屏")
            }
        }
    }

    private fun refreshServiceStatus() {
        deviceIpState = NetworkUtil.getLocalIpAddress()
        // 尝试绑定服务
        try {
            bindToService()
        } catch (_: Exception) {}
    }

    private fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to application details if direct setting fails
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }
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
        serverStatusState = ServerStatus.STOPPED
        wakeLockHeldState = false
        wifiLockHeldState = false
        ringActiveState = false
        flashModeState = "off"
        nsdStatusState = NsdStatus.IDLE
    }

    override fun onResume() {
        super.onResume()
        refreshServiceStatus()
    }

    override fun onDestroy() {
        nsdDiscoveryManager.stopDiscovery()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        super.onDestroy()
    }
}
