package com.reborn.core.network.model.response.place

import kotlinx.serialization.Serializable

@Serializable
data class PlaceDetailResponse(
    val placeId: Long,
    val name: String,
    val type: String,
    val accessLevel: String,
    val deviceCount: Int,
    val qrCode: String,
    val createdAt: String,
)
