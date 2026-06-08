package com.reborn.server.domain.device.controller

import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.service.DeviceService
import com.reborn.server.global.model.ApiResponse
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/device")
class DeviceController(
    private val deviceService: DeviceService,
) {

    @PostMapping
    fun register(
        @RequestBody request: DeviceDto.RegisterRequest,
        authentication: Authentication,
    ): ApiResponse<DeviceDto.RegisterResponse> =
        ApiResponse.success(deviceService.register(authentication.principal as Long, request))
}
