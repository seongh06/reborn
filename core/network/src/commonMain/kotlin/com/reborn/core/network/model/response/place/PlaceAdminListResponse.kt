package com.reborn.core.network.model.response.place

import kotlinx.serialization.Serializable

@Serializable
data class PlaceAdminItemResponse(
    val userId: Long,
    val name: String,
    val profileImage: String? = null,
    val isOwner: Boolean = false,
)

@Serializable
data class PlaceAdminListResponse(
    val admins: List<PlaceAdminItemResponse>,
)
