package com.reborn.server.domain.auth.controller

import com.reborn.server.domain.auth.dto.AuthDto
import com.reborn.server.domain.auth.service.AuthService
import com.reborn.server.global.model.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "인증 API", description = "카카오/구글 소셜 로그인")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {

    @Operation(
        summary = "구글 로그인",
        description = "Google OAuth 2.0 idToken을 검증하여 로그인 또는 회원가입을 처리합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "로그인 성공 — accessToken, refreshToken, userId, name, isNewUser 반환"),
        SwaggerApiResponse(responseCode = "400", description = "idToken 누락"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Google ID Token"),
        SwaggerApiResponse(responseCode = "409", description = "이미 다른 소셜 계정으로 가입된 이메일"),
    )
    @PostMapping("/google")
    fun google(@Valid @RequestBody request: AuthDto.GoogleLoginRequest): ApiResponse<AuthDto.LoginResponse> =
        ApiResponse.success(authService.loginWithGoogle(request))

    @Operation(
        summary = "카카오 로그인",
        description = "Kakao OAuth 2.0 accessToken을 검증하여 로그인 또는 회원가입을 처리합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "로그인 성공 — accessToken, refreshToken, userId, name, isNewUser 반환"),
        SwaggerApiResponse(responseCode = "400", description = "accessToken 누락"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Kakao AccessToken"),
        SwaggerApiResponse(responseCode = "409", description = "이미 다른 소셜 계정으로 가입된 이메일"),
    )
    @PostMapping("/kakao")
    fun kakao(@Valid @RequestBody request: AuthDto.KakaoLoginRequest): ApiResponse<AuthDto.LoginResponse> =
        ApiResponse.success(authService.loginWithKakao(request))
}
