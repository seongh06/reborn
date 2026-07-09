package com.reborn.core.network.model.response.place

import kotlinx.serialization.Serializable

@Serializable
data class PlaceListResponse(
    val places: List<PlaceItemResponse>,
)

@Serializable
data class PlaceItemResponse(
    val placeId: Long,
    val name: String,
    val type: String,
    val accessLevel: String,
    val createdAt: String,
)
