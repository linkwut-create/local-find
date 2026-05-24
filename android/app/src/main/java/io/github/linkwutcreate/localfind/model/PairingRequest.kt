package io.github.linkwutcreate.localfind.model

data class PairingRequest(
    val requestId: String,
    val controllerId: String,
    val controllerName: String,
    val controllerType: String,
    val nonce: String,
    val status: String,
    val createdAt: Long,
    val expiresAt: Long,
    val controlToken: String? = null,
)
