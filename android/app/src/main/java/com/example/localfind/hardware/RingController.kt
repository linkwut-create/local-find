package com.example.localfind.hardware

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
}
