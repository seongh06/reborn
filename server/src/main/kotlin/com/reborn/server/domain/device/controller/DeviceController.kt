package com.reborn.server.domain.device.controller

import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.service.DeviceService
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "기기 API", description = "IoT 기기 등록 및 관리 (ADMIN 권한 필요)")
@RestController
@RequestMapping("/api/device")
class DeviceController(
    private val deviceService: DeviceService,
    private val smartThingsDeviceService: SmartThingsDeviceService,
) {

    @Operation(
        summary = "Arduino/AI 스피커 기기 등록",
        description = "특정 장소에 Arduino IoT 기기 또는 AI 스피커(ATOM ECHO) 기기를 등록합니다. " +
            "deviceType을 생략하면 ARDUINO로 등록되며, AI 스피커는 deviceType=AI_SPEAKER로 명시해야 합니다. " +
            "해당 장소의 ADMIN 권한이 필요하며, AEROMETER(공기계)/SMART_THINGS 등록은 각각 페어링 코드/OAuth 방식으로 별도 처리됩니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "등록 성공 — deviceId, 기기명, 기기 유형(ARDUINO/AI_SPEAKER), 등록일시 반환"),
        SwaggerApiResponse(responseCode = "400", description = "필수 필드 누락(placeId, deviceId, deviceName) 또는 지원하지 않는 deviceType"),
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
    ): ApiResponse<DeviceDto.RegisterResponse> =
        ApiResponse.success(deviceService.register(extractUserId(authentication), request))

    @Operation(
        summary = "페어링 코드 생성",
        description = "공기계(AEROMETER) 앱과 장소를 연결하기 위한 일회용 페어링 코드를 생성합니다(10분 유효). 해당 장소의 ADMIN 권한이 필요합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "생성 성공 — pairingCode, expiresAt 반환"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        SwaggerApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 장소"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/pairing/code")
    fun generatePairingCode(
        @RequestParam placeId: Long,
        authentication: Authentication,
    ): ApiResponse<DeviceDto.PairingCodeResponse> =
        ApiResponse.success(deviceService.generatePairingCode(extractUserId(authentication), placeId))

    @Operation(
        summary = "페어링 코드 입력",
        description = "공기계 앱에서 페어링 코드를 입력해 장소와 기기를 연결합니다. 공기계 앱은 별도 로그인을 하지 않으므로 " +
            "인증이 필요 없으며, 페어링 코드 자체가 유일한 인가 수단입니다. 검증 성공 시 AEROMETER 기기가 등록되고 appToken이 발급됩니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "페어링 성공 — deviceId, placeId, appToken 반환"),
        SwaggerApiResponse(responseCode = "400", description = "코드 누락 또는 만료된 코드"),
        SwaggerApiResponse(responseCode = "409", description = "이미 등록된 기기"),
    )
    @PostMapping("/pairing")
    fun pairDevice(@Valid @RequestBody request: DeviceDto.PairingRequest): ApiResponse<DeviceDto.PairingResponse> =
        ApiResponse.success(deviceService.pairDevice(request))

    @Operation(
        summary = "기기 목록 조회",
        description = "특정 장소에 등록된 기기(ARDUINO/AEROMETER) 목록을 조회합니다. 해당 장소의 ADMIN 권한이 필요합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공 — 기기 목록 반환"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        SwaggerApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 장소"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    fun getList(
        @RequestParam placeId: Long,
        authentication: Authentication,
    ): ApiResponse<DeviceDto.ListResponse> =
        ApiResponse.success(deviceService.getList(extractUserId(authentication), placeId))

    @Operation(
        summary = "IoT 기기 제어",
        description = "SmartThings로 등록된 기기(SMART_THINGS 타입)에 제어 명령을 보냅니다. 서버가 해당 " +
            "장소의 SmartThings 토큰으로 SmartThings API를 직접 호출합니다(#132). 전원/운전모드/온도/풍량 중 " +
            "보낸 필드만 반영됩니다. 해당 장소의 ADMIN 권한이 필요합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "제어 명령 전달 완료"),
        SwaggerApiResponse(responseCode = "400", description = "제어할 항목 없음 또는 SmartThings 기기가 아님"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        SwaggerApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 기기 또는 이 장소에 SmartThings 미연동"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{deviceId}/control")
    fun control(
        @PathVariable deviceId: String,
        @RequestBody request: DeviceDto.ControlRequest,
        authentication: Authentication,
    ): ApiResponse<DeviceDto.ControlResponse> =
        ApiResponse.success(smartThingsDeviceService.control(extractUserId(authentication), deviceId, request))

    private fun extractUserId(authentication: Authentication): Long =
        authentication.principal as? Long
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "인증 정보가 유효하지 않습니다.")
}
