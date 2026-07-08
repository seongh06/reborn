package com.reborn.server.domain.auth.controller

import com.reborn.server.domain.auth.dto.AuthDto
import com.reborn.server.domain.auth.service.AuthService
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.ApiResponse
import com.reborn.server.global.model.CommonErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "인증 API", description = "소셜 로그인 (통합)")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {

    @Operation(
        summary = "소셜 로그인 (통합)",
        description = "provider(GOOGLE/KAKAO)에 따라 Google idToken 또는 Kakao accessToken을 검증하여 " +
            "로그인 또는 회원가입을 처리합니다. 기존 /google, /kakao 엔드포인트를 이 API로 통합함.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "로그인 성공 — accessToken, refreshToken, userId, name, isNewUser 반환"),
        SwaggerApiResponse(responseCode = "400", description = "provider/token 누락 또는 지원하지 않는 provider"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 소셜 토큰"),
        SwaggerApiResponse(responseCode = "409", description = "이미 다른 소셜 계정으로 가입된 이메일"),
    )
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: AuthDto.LoginRequest): ApiResponse<AuthDto.LoginResponse> =
        ApiResponse.success(authService.login(request))

    @Operation(
        summary = "AccessToken 재발급",
        description = "RefreshToken을 검증하여 AccessToken/RefreshToken을 재발급합니다(Token Rotation). " +
            "요청 RefreshToken이 Redis에 저장된 최신 값과 다르면 거부합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "재발급 성공 — 새 accessToken, refreshToken 반환"),
        SwaggerApiResponse(responseCode = "400", description = "refreshToken 누락"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않거나 만료되었거나 최신이 아닌 RefreshToken"),
    )
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: AuthDto.RefreshRequest): ApiResponse<AuthDto.RefreshResponse> =
        ApiResponse.success(authService.refresh(request))

    @Operation(
        summary = "로그아웃",
        description = "인증된 사용자의 RefreshToken을 Redis에서 삭제합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "로그아웃 완료"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 AccessToken"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    fun logout(authentication: Authentication): ApiResponse<Nothing> {
        authService.logout(extractUserId(authentication))
        return ApiResponse.success("정상적으로 로그아웃되었습니다.")
    }

    @Operation(
        summary = "FCM 토큰 갱신",
        description = "앱 재설치 또는 토큰 만료 시 새로운 FCM 토큰을 서버에 업데이트합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "FCM 토큰 갱신 완료"),
        SwaggerApiResponse(responseCode = "400", description = "fcmToken 누락"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 AccessToken"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 회원"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/fcm")
    fun updateFcmToken(
        @Valid @RequestBody request: AuthDto.FcmTokenUpdateRequest,
        authentication: Authentication,
    ): ApiResponse<Nothing> {
        authService.updateFcmToken(extractUserId(authentication), request)
        return ApiResponse.success("FCM 토큰이 정상적으로 갱신되었습니다.")
    }

    private fun extractUserId(authentication: Authentication): Long =
        authentication.principal as? Long
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "인증 정보가 유효하지 않습니다.")
}
