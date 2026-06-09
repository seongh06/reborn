package com.reborn.server.domain.data.controller

import com.reborn.server.domain.data.dto.SensorDataDto
import com.reborn.server.domain.data.service.SensorDataService
import com.reborn.server.global.model.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "센서 데이터 API", description = "Arduino IoT 기기의 센서 데이터 수집 및 최신 데이터 조회")
@RestController
@RequestMapping("/api/sensor")
class SensorDataController(
    private val sensorDataService: SensorDataService,
) {

    @Operation(summary = "센서 데이터 수집", description = "Arduino 기기가 온도·습도·조도·재실 인원 데이터를 서버에 전송합니다. 헤더의 X-Device-Id로 기기를 식별합니다.")
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "수집 성공 — logId, 불쾌지수, 수집일시 반환"),
        SwaggerApiResponse(responseCode = "400", description = "센서 값이 모두 비어있거나 유효 범위를 벗어난 경우"),
        SwaggerApiResponse(responseCode = "404", description = "등록되지 않은 기기 ID"),
    )
    @PostMapping("/collect")
    fun collect(
        @RequestHeader("X-Device-Id") deviceId: String,
        @RequestBody request: SensorDataDto.CollectRequest,
    ): ApiResponse<SensorDataDto.CollectResponse> =
        ApiResponse.success(sensorDataService.collect(deviceId, request))

    @Operation(summary = "최신 센서 데이터 조회", description = "특정 기기의 가장 최근 센서 로그 1건을 조회합니다.")
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공 — 온도·습도·조도·재실 인원·불쾌지수 반환"),
        SwaggerApiResponse(responseCode = "404", description = "등록되지 않은 기기이거나 수집된 데이터 없음"),
    )
    @GetMapping("/current")
    fun getCurrent(
        @RequestParam deviceId: String,
    ): ApiResponse<SensorDataDto.CurrentResponse> =
        ApiResponse.success(sensorDataService.getCurrent(deviceId))
}
