package com.reborn.core.network.model.response.device

import kotlinx.serialization.Serializable

@Serializable
data class PairingCodeResponse(
    val pairingCode: String,
    val expiresAt: String,
)
