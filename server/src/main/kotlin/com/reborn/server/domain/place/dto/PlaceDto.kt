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

    data class PairingCodeResponse(
        val pairingCode: String,
        val expiresAt: LocalDateTime,
    )

    data class PairingRequest(
        @field:NotBlank val pairingCode: String? = null,
        @field:NotBlank val deviceName: String? = null,
    )

    data class PairingResponse(
        val deviceId: String,
        val placeId: Long,
        val appToken: String,
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
