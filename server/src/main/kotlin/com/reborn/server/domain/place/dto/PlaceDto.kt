package com.reborn.server.domain.place.dto

import java.time.LocalDateTime

class PlaceDto {

    data class RegisterRequest(
        val name: String? = null,
        val type: String? = null,
    )

    data class RegisterResponse(
        val placeId: Long,
        val name: String,
        val type: String,
        val createdAt: LocalDateTime,
    )
}
