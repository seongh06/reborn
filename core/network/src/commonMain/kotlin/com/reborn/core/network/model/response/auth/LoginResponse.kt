package com.reborn.core.network.model.response.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
    val name: String,
    val isNewUser: Boolean
)