package com.reborn.core.network.model.response.smartthings

import kotlinx.serialization.Serializable

@Serializable
data class AuthorizeResponse(
    val authorizeUrl: String,
)
