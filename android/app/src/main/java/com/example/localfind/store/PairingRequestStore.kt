package com.example.localfind.store

import android.content.Context
import com.example.localfind.model.PairingRequest
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PairingRequestStore(context: Context) {
    private val prefs = context.getSharedPreferences("pairing_requests", Context.MODE_PRIVATE)

    @Synchronized
    fun create(
        controllerId: String,
        controllerName: String,
        controllerType: String,
        nonce: String,
        ttlMillis: Long,
    ): PairingRequest {
        expireOldRequests()
        val now = System.currentTimeMillis()
        val request = PairingRequest(
            requestId = UUID.randomUUID().toString(),
            controllerId = controllerId,
            controllerName = controllerName,
            controllerType = controllerType,
            nonce = nonce,
            status = STATUS_PENDING,
            createdAt = now,
            expiresAt = now + ttlMillis,
        )
        saveAll(loadAll().filterNot { it.requestId == request.requestId } + request)
        return request
    }

    @Synchronized
    fun get(requestId: String): PairingRequest? {
        expireOldRequests()
        return loadAll().firstOrNull { it.requestId == requestId }
    }

    @Synchronized
    fun getPending(): List<PairingRequest> {
        expireOldRequests()
        return loadAll().filter { it.status == STATUS_PENDING }
    }

    @Synchronized
    fun accept(requestId: String, controlToken: String): PairingRequest? {
        return update(requestId) { it.copy(status = STATUS_ACCEPTED, controlToken = controlToken) }
    }

    @Synchronized
    fun reject(requestId: String): PairingRequest? {
        return update(requestId) { it.copy(status = STATUS_REJECTED, controlToken = null) }
    }

    @Synchronized
    fun expireOldRequests(now: Long = System.currentTimeMillis()) {
        val updated = loadAll().map { request ->
            if (request.status == STATUS_PENDING && request.expiresAt <= now) {
                request.copy(status = STATUS_EXPIRED)
            } else {
                request
            }
        }
        saveAll(updated)
    }

    private fun update(requestId: String, transform: (PairingRequest) -> PairingRequest): PairingRequest? {
        expireOldRequests()
        var updatedRequest: PairingRequest? = null
        val updated = loadAll().map { request ->
            if (request.requestId == requestId) {
                transform(request).also { updatedRequest = it }
            } else {
                request
            }
        }
        saveAll(updated)
        return updatedRequest
    }

    private fun loadAll(): List<PairingRequest> {
        val raw = prefs.getString(KEY_REQUESTS, "[]") ?: "[]"
        val array = try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val requestId = item.optString("requestId")
                if (requestId.isBlank()) continue
                add(
                    PairingRequest(
                        requestId = requestId,
                        controllerId = item.optString("controllerId"),
                        controllerName = item.optString("controllerName", "Chrome Extension"),
                        controllerType = item.optString("controllerType", "chrome_extension"),
                        nonce = item.optString("nonce"),
                        status = item.optString("status", STATUS_PENDING),
                        createdAt = item.optLong("createdAt", 0L),
                        expiresAt = item.optLong("expiresAt", 0L),
                        controlToken = item.optString("controlToken").takeIf { it.isNotBlank() },
                    )
                )
            }
        }
    }

    private fun saveAll(requests: List<PairingRequest>) {
        val array = JSONArray()
        requests.forEach { request ->
            val item = JSONObject()
                .put("requestId", request.requestId)
                .put("controllerId", request.controllerId)
                .put("controllerName", request.controllerName)
                .put("controllerType", request.controllerType)
                .put("nonce", request.nonce)
                .put("status", request.status)
                .put("createdAt", request.createdAt)
                .put("expiresAt", request.expiresAt)
            request.controlToken?.let { item.put("controlToken", it) }
            array.put(item)
        }
        prefs.edit().putString(KEY_REQUESTS, array.toString()).apply()
    }

    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_EXPIRED = "expired"

        private const val KEY_REQUESTS = "requests"
    }
}
