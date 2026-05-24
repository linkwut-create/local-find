package io.github.linkwutcreate.localfind.model

data class LocalDeviceIdentity(
    val id: String,
    val name: String,
    val type: String = "android_phone",
    val createdAt: Long,
)
