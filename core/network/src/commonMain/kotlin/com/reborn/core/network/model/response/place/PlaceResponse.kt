package com.reborn.core.network.model.response.place

import kotlinx.serialization.Serializable

@Serializable
data class PlaceResponse(
    val placeId: Long,
    val name: String,
    val type: String,
    val createdAt: String,
)
