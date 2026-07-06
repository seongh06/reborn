package com.reborn.server.domain.auth.dto

import jakarta.validation.constraints.NotBlank

class AuthDto {

    data class GoogleLoginRequest(
        @field:NotBlank val idToken: String? = null,
    )

    data class KakaoLoginRequest(
        @field:NotBlank val accessToken: String? = null,
    )

    data class LoginResponse(
        val accessToken: String,
        val refreshToken: String,
        val userId: Long,
        val name: String,
        val isNewUser: Boolean,
    )
}
