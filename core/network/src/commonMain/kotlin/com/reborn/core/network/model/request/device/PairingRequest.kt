package com.reborn.core.network.model.request.device

import kotlinx.serialization.Serializable

@Serializable
data class PairingRequest(
    val pairingCode: String,
    val deviceName: String,
)
