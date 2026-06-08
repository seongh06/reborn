package com.reborn.server.domain.data.controller

import com.reborn.server.domain.data.dto.SensorDataDto
import com.reborn.server.domain.data.service.SensorDataService
import com.reborn.server.global.model.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sensor")
class SensorDataController(
    private val sensorDataService: SensorDataService,
) {

    @PostMapping("/collect")
    fun collect(
        @RequestHeader("X-Device-Id") deviceId: String,
        @RequestBody request: SensorDataDto.CollectRequest,
    ): ApiResponse<SensorDataDto.CollectResponse> =
        ApiResponse.success(sensorDataService.collect(deviceId, request))

    @GetMapping("/current")
    fun getCurrent(
        @RequestParam deviceId: String,
    ): ApiResponse<SensorDataDto.CurrentResponse> =
        ApiResponse.success(sensorDataService.getCurrent(deviceId))
}
