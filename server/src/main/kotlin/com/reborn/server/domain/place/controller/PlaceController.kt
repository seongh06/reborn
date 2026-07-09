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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
    ): ApiResponse<PlaceDto.RegisterResponse> =
        ApiResponse.success(placeService.register(extractUserId(authentication), request))

    @Operation(
        summary = "관리자 코드 생성",
        description = "특정 장소에 새로운 관리자를 초대하기 위한 일회용 코드를 생성합니다(30분 유효). 해당 장소의 ADMIN 권한이 필요합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "생성 성공 — adminCode, expiresAt 반환"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        SwaggerApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 장소"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/admin/code")
    fun generateAdminCode(
        @RequestParam placeId: Long,
        authentication: Authentication,
    ): ApiResponse<PlaceDto.AdminCodeResponse> =
        ApiResponse.success(placeService.generateAdminCode(extractUserId(authentication), placeId))

    @Operation(
        summary = "관리자 코드 입력",
        description = "초대받은 사용자가 관리자 코드를 입력해 해당 장소의 ADMIN 권한을 획득합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "권한 부여 성공 — placeId, placeName, accessLevel 반환"),
        SwaggerApiResponse(responseCode = "400", description = "코드 누락 또는 만료된 코드"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        SwaggerApiResponse(responseCode = "409", description = "이미 해당 장소에 등록된 사용자"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/admin")
    fun redeemAdminCode(
        @Valid @RequestBody request: PlaceDto.AdminInviteRequest,
        authentication: Authentication,
    ): ApiResponse<PlaceDto.AdminInviteResponse> =
        ApiResponse.success(placeService.redeemAdminCode(extractUserId(authentication), request))

    @Operation(
        summary = "장소 목록 조회",
        description = "인증된 사용자가 속한 모든 장소 목록을 내 권한(ADMIN/USER)과 함께 조회합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공 — placeId, name, type, accessLevel, createdAt 목록 반환"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    fun getList(authentication: Authentication): ApiResponse<PlaceDto.ListResponse> =
        ApiResponse.success(placeService.getList(extractUserId(authentication)))

    @Operation(
        summary = "장소 상세 조회",
        description = "특정 장소의 상세 정보(이름, 유형, 내 권한, 등록된 기기 수, QR 코드)를 조회합니다. 해당 장소에 대한 접근 권한이 필요합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(
            responseCode = "200",
            description = "조회 성공 — placeId, name, type, accessLevel, deviceCount, qrCode, createdAt 반환",
        ),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        SwaggerApiResponse(responseCode = "403", description = "해당 장소 접근 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 장소"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{placeId}")
    fun getDetail(
        @PathVariable placeId: Long,
        authentication: Authentication,
    ): ApiResponse<PlaceDto.DetailResponse> =
        ApiResponse.success(placeService.getDetail(extractUserId(authentication), placeId))

    @Operation(
        summary = "장소 삭제",
        description = "등록된 장소를 삭제합니다. 삭제 시 기기·사용자 매핑 정보가 CASCADE로 함께 삭제됩니다. (ADMIN 권한 필요)",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "삭제 성공"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        SwaggerApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 장소"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{placeId}")
    fun delete(
        @PathVariable placeId: Long,
        authentication: Authentication,
    ): ApiResponse<Nothing> {
        placeService.deletePlace(extractUserId(authentication), placeId)
        return ApiResponse.success("장소가 삭제되었습니다.")
    }

    private fun extractUserId(authentication: Authentication): Long =
        authentication.principal as? Long
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "인증 정보가 유효하지 않습니다.")
}
