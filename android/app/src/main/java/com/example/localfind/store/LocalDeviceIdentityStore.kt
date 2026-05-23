package com.example.localfind.store

import android.content.Context
import android.os.Build
import com.example.localfind.model.LocalDeviceIdentity
import java.util.UUID

class LocalDeviceIdentityStore(context: Context) {
    private val prefs = context.getSharedPreferences("local_device_identity", Context.MODE_PRIVATE)

    @Synchronized
    fun getOrCreate(): LocalDeviceIdentity {
        val existingId = prefs.getString(KEY_ID, null)
        val existingCreatedAt = prefs.getLong(KEY_CREATED_AT, 0L)
        if (!existingId.isNullOrBlank() && existingCreatedAt > 0L) {
            return LocalDeviceIdentity(
                id = existingId,
                name = prefs.getString(KEY_NAME, null).orFallbackName(),
                type = prefs.getString(KEY_TYPE, TYPE_ANDROID_PHONE) ?: TYPE_ANDROID_PHONE,
                createdAt = existingCreatedAt,
            )
        }

        val identity = LocalDeviceIdentity(
            id = UUID.randomUUID().toString(),
            name = Build.MODEL.orFallbackName(),
            type = TYPE_ANDROID_PHONE,
            createdAt = System.currentTimeMillis(),
        )
        prefs.edit()
            .putString(KEY_ID, identity.id)
            .putString(KEY_NAME, identity.name)
            .putString(KEY_TYPE, identity.type)
            .putLong(KEY_CREATED_AT, identity.createdAt)
            .apply()
        return identity
    }

    private fun String?.orFallbackName(): String {
        return this?.takeIf { it.isNotBlank() } ?: "Android Phone"
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_TYPE = "type"
        private const val KEY_CREATED_AT = "createdAt"
        private const val TYPE_ANDROID_PHONE = "android_phone"
    }
}
