package com.reborn.server.domain.auth.dto

import jakarta.validation.constraints.NotBlank

class AuthDto {

    data class LoginRequest(
        @field:NotBlank val provider: String? = null,
        @field:NotBlank val token: String? = null,
    )

    data class RefreshRequest(
        @field:NotBlank val refreshToken: String? = null,
    )

    data class RefreshResponse(
        val accessToken: String,
        val refreshToken: String,
    )

    data class FcmTokenUpdateRequest(
        @field:NotBlank val fcmToken: String? = null,
    )

    data class LoginResponse(
        val accessToken: String,
        val refreshToken: String,
        val userId: Long,
        val name: String,
        val isNewUser: Boolean,
    )
}
