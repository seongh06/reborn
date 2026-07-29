package com.reborn.core.network.model.response.place

import kotlinx.serialization.Serializable

@Serializable
data class PlaceWifiResponse(
    val ssid: String? = null,
    val password: String? = null,
)
