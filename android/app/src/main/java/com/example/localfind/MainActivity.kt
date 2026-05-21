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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.localfind.service.FindPhoneForegroundService
import com.example.localfind.ui.MainScreen
import com.example.localfind.util.NetworkUtil

class MainActivity : ComponentActivity() {

    private var foregroundService: FindPhoneForegroundService? = null
    private var isServiceBound by mutableStateOf(false)

    // 反射给 Jetpack Compose 驱动的响应式核心状态
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
        // 尝试绑定服务
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
}
