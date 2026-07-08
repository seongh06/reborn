package com.reborn.server.domain.place.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

class PlaceDto {

    data class RegisterRequest(
        @field:NotBlank val name: String? = null,
        @field:NotBlank val type: String? = null,
    )

    data class RegisterResponse(
        val placeId: Long,
        val name: String,
        val type: String,
        val createdAt: LocalDateTime,
    )

    data class AdminCodeResponse(
        val adminCode: String,
        val expiresAt: LocalDateTime,
    )

    data class AdminInviteRequest(
        @field:NotBlank val adminCode: String? = null,
    )

    data class AdminInviteResponse(
        val placeId: Long,
        val placeName: String,
        val accessLevel: String,
    )
}
