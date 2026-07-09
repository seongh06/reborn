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

    data class PlaceItem(
        val placeId: Long,
        val name: String,
        val type: String,
        val accessLevel: String,
        val createdAt: LocalDateTime,
    )

    data class ListResponse(
        val places: List<PlaceItem>,
    )

    data class DetailResponse(
        val placeId: Long,
        val name: String,
        val type: String,
        val accessLevel: String,
        val deviceCount: Int,
        val qrCode: String,
        val createdAt: LocalDateTime,
    )
}
