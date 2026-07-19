package com.reborn.server.domain.smartthings.controller

import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.smartthings.dto.SmartThingsDto
import com.reborn.server.domain.smartthings.service.SmartThingsDeviceService
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "SmartThings 기기 API", description = "장소에 연동된 SmartThings 계정의 기기 조회·등록 (#132)")
@RestController
@RequestMapping("/api/smartthings/devices")
class SmartThingsDeviceController(
    private val smartThingsDeviceService: SmartThingsDeviceService,
) {

    @Operation(
        summary = "SmartThings 기기 목록 조회",
        description = "장소에 연동된 SmartThings 계정에서 조회 가능한 전체 기기 목록을 가져옵니다. " +
            "이 중 관리자가 이 장소에서 쓸 기기를 골라 등록(POST)하는 흐름입니다. 해당 장소의 ADMIN 권한이 필요합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        SwaggerApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 장소"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    fun list(
        @RequestParam placeId: Long,
        authentication: Authentication,
    ): ApiResponse<SmartThingsDto.DeviceListResponse> =
        ApiResponse.success(smartThingsDeviceService.listDevices(extractUserId(authentication), placeId))

    @Operation(
        summary = "SmartThings 기기 등록",
        description = "SmartThings 기기 목록 중 선택한 기기를 이 장소의 제어 대상으로 등록합니다. " +
            "`device` 테이블에 SMART_THINGS 타입으로 저장됩니다. 해당 장소의 ADMIN 권한이 필요합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "등록 성공"),
        SwaggerApiResponse(responseCode = "400", description = "필수 필드 누락"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        SwaggerApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 장소"),
        SwaggerApiResponse(responseCode = "409", description = "이미 등록된 기기"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    fun register(
        @Valid @RequestBody request: SmartThingsDto.RegisterDeviceRequest,
        authentication: Authentication,
    ): ApiResponse<DeviceDto.RegisterResponse> =
        ApiResponse.success(smartThingsDeviceService.registerDevice(extractUserId(authentication), request))

    private fun extractUserId(authentication: Authentication): Long =
        authentication.principal as? Long
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "인증 정보가 유효하지 않습니다.")
}
