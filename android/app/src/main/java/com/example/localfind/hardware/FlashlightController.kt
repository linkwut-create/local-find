package com.example.localfind.hardware

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
        cancelStrobe()
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
     * 使用协程控制，并在 job 取销时确保手电筒还原关闭
     */
    @Synchronized
    fun startStrobe() {
        cancelStrobe()
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

    private fun cancelStrobe() {
        strobeJob?.cancel()
        strobeJob = null
    }

    /**
     * 停止全部动作 (关闭手电筒、取消闪烁协程)
     */
    @Synchronized
    fun stopAll() {
        cancelStrobe()
        
        val id = cameraId ?: return
        try {
            cameraManager.setTorchMode(id, false)
            isSteadyOn = false
            Log.d("FlashlightController", "Stopped both strobe and steady flashlight modes")
        } catch (e: Exception) {
            Log.e("FlashlightController", "Failed to force set torch mode to false", e)
        }
    }
}
