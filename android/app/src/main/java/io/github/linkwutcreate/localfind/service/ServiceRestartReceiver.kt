package io.github.linkwutcreate.localfind.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Restores an explicitly enabled finder service after boot or an APK update.
 * OEM battery/freeze policies can still block or later kill the process; the
 * in-app guidance remains required on those devices.
 */
class ServiceRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        if (!ServiceRunState.shouldRun(context)) {
            return
        }

        val serviceIntent = Intent(context, FindPhoneForegroundService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d("ServiceRestartReceiver", "Requested finder service restart for $action")
        } catch (error: Exception) {
            Log.e("ServiceRestartReceiver", "Unable to restart finder service for $action", error)
        }
    }
}
