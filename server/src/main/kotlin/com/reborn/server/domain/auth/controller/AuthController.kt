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
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

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

    @Operation(
        summary = "내 프로필 조회",
        description = "인증된 사용자의 이름/프로필 이미지를 조회합니다. Setting 화면 상단 Profile 섹션(#155)에서 사용.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 AccessToken"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 회원"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    fun me(authentication: Authentication): ApiResponse<AuthDto.MeResponse> =
        ApiResponse.success(authService.getMe(extractUserId(authentication)))

    @Operation(
        summary = "내 프로필 수정",
        description = "인증된 사용자의 이름(닉네임)을 수정합니다(#177, Setting 화면 프로필 편집).",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "수정 성공"),
        SwaggerApiResponse(responseCode = "400", description = "name 누락"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 AccessToken"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 회원"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/me")
    fun updateMe(
        @Valid @RequestBody request: AuthDto.UpdateProfileRequest,
        authentication: Authentication,
    ): ApiResponse<AuthDto.MeResponse> =
        ApiResponse.success(authService.updateProfile(extractUserId(authentication), request))

    @Operation(
        summary = "내 프로필 이미지 변경",
        description = "인증된 사용자의 프로필 이미지를 업로드하여 교체합니다(S3). JPEG/PNG/WEBP, 최대 5MB. " +
            "S3 자격증명이 설정되지 않은 환경에서는 500으로 실패합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "변경 성공"),
        SwaggerApiResponse(responseCode = "400", description = "이미지 파일 누락/형식 오류/용량 초과"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 AccessToken"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 회원"),
        SwaggerApiResponse(responseCode = "500", description = "S3 업로드 미설정 환경"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/me/profile-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateProfileImage(
        @RequestPart("image") image: MultipartFile,
        authentication: Authentication,
    ): ApiResponse<AuthDto.MeResponse> =
        ApiResponse.success(authService.updateProfileImage(extractUserId(authentication), image))

    @Operation(
        summary = "회원 탈퇴",
        description = "인증된 사용자를 탈퇴 처리합니다. 사용자가 어떤 장소의 유일한 ADMIN이면 " +
            "(다른 관리자가 없으면) 차단됩니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "탈퇴 완료"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 AccessToken"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 회원"),
        SwaggerApiResponse(responseCode = "409", description = "유일한 관리자로 등록된 장소가 있어 탈퇴 불가"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/me")
    fun withdraw(authentication: Authentication): ApiResponse<Nothing> {
        authService.withdraw(extractUserId(authentication))
        return ApiResponse.success("정상적으로 탈퇴되었습니다.")
    }

    private fun extractUserId(authentication: Authentication): Long =
        authentication.principal as? Long
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "인증 정보가 유효하지 않습니다.")
}
