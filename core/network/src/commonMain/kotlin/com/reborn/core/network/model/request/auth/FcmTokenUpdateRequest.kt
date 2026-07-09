package com.reborn.core.network.model.request.auth

import kotlinx.serialization.Serializable

@Serializable
data class FcmTokenUpdateRequest(
    val fcmToken: String
)
