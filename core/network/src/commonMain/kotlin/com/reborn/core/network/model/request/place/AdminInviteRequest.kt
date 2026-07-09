package com.reborn.core.network.model.request.place

import kotlinx.serialization.Serializable

@Serializable
data class AdminInviteRequest(
    val adminCode: String,
)
