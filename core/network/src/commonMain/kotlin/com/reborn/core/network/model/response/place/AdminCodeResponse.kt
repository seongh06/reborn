package com.reborn.core.network.model.response.place

import kotlinx.serialization.Serializable

@Serializable
data class AdminCodeResponse(
    val adminCode: String,
    val expiresAt: String,
)
