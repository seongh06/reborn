package com.reborn.server.domain.place.controller

import com.reborn.server.domain.place.dto.PlaceDto
import com.reborn.server.domain.place.service.PlaceService
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "장소 API", description = "장소 등록 및 관리 (인증 필요)")
@RestController
@RequestMapping("/api/place")
class PlaceController(
    private val placeService: PlaceService,
) {

    @Operation(
        summary = "장소 등록",
        description = "새로운 장소를 등록합니다. 등록한 사용자에게 자동으로 ADMIN 권한이 부여됩니다. " +
            "공간 유형은 HOME·STORE·COMPANY 중 하나를 입력하세요.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "등록 성공 — placeId, 장소명, 공간 유형, 생성일시 반환"),
        SwaggerApiResponse(responseCode = "400", description = "장소 이름 누락 또는 정의되지 않은 공간 유형"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않거나 만료된 AccessToken)"),
        SwaggerApiResponse(responseCode = "404", description = "토큰에서 추출한 userId에 해당하는 회원 없음"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    fun register(
        @Valid @RequestBody request: PlaceDto.RegisterRequest,
        authentication: Authentication,
    ): ApiResponse<PlaceDto.RegisterResponse> {
        val userId = authentication.principal as? Long
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "인증 정보가 유효하지 않습니다.")
        return ApiResponse.success(placeService.register(userId, request))
    }
}
