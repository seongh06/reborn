package com.reborn.core.network.model.response.auth

import kotlinx.serialization.Serializable

@Serializable
data class MeResponse(
    val userId: Long,
    val name: String,
    val profileImage: String?
)
