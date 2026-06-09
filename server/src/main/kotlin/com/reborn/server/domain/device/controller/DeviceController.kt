package com.reborn.server.domain.device.controller

import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.service.DeviceService
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

@Tag(name = "기기 API", description = "IoT 기기 등록 및 관리 (ADMIN 권한 필요)")
@RestController
@RequestMapping("/api/device")
class DeviceController(
    private val deviceService: DeviceService,
) {

    @Operation(summary = "Arduino 기기 등록", description = "특정 장소에 Arduino IoT 기기를 등록합니다. 해당 장소의 ADMIN 권한이 필요하며, KIOSK(공기계) 등록은 페어링 코드 방식으로 별도 처리됩니다.")
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "등록 성공 — deviceId, 기기명, 기기 유형(ARDUINO), 등록일시 반환"),
        SwaggerApiResponse(responseCode = "400", description = "필수 필드 누락 (placeId, deviceId, deviceName)"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않거나 만료된 AccessToken)"),
        SwaggerApiResponse(responseCode = "403", description = "해당 장소의 ADMIN 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 장소 ID"),
        SwaggerApiResponse(responseCode = "409", description = "이미 등록된 deviceId"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    fun register(
        @Valid @RequestBody request: DeviceDto.RegisterRequest,
        authentication: Authentication,
    ): ApiResponse<DeviceDto.RegisterResponse> {
        val userId = authentication.principal as? Long
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "인증 정보가 유효하지 않습니다.")
        return ApiResponse.success(deviceService.register(userId, request))
    }
}
