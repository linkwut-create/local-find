package io.github.linkwutcreate.localfind.model

data class PairedControllerToken(
    val controllerId: String,
    val controllerName: String,
    val controllerType: String,
    val token: String,
    val pairedAt: Long,
)
