package com.reborn.core.network.model.response.place

import kotlinx.serialization.Serializable

@Serializable
data class AdminInviteResponse(
    val placeId: Long,
    val placeName: String,
    val accessLevel: String,
)
