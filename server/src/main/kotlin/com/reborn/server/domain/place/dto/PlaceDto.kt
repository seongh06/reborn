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
}
