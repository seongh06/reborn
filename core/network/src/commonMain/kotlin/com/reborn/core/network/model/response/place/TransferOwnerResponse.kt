package com.reborn.core.network.model.response.place

import kotlinx.serialization.Serializable

@Serializable
data class TransferOwnerResponse(
    val placeId: Long,
    val newOwnerUserId: Long,
)
