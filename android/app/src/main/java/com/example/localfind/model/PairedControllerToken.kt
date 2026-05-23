package com.example.localfind.model

data class PairedControllerToken(
    val controllerId: String,
    val controllerName: String,
    val controllerType: String,
    val token: String,
    val pairedAt: Long,
)
