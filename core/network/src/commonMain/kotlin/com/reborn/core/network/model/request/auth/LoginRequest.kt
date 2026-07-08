package com.reborn.core.network.model.request.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val provider: String,
    val token: String
)