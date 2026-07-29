package com.reborn.core.network.model.request.place

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePlaceWifiRequest(
    val ssid: String,
    val password: String,
)
