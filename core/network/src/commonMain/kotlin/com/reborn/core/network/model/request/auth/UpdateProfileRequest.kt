package com.reborn.core.network.model.request.auth

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val name: String,
)
