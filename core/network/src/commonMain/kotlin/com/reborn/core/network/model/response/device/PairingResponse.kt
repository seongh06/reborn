package com.reborn.core.network.model.response.device

import kotlinx.serialization.Serializable

@Serializable
data class PairingResponse(
    val deviceId: String,
    val placeId: Long,
    val appToken: String,
)
