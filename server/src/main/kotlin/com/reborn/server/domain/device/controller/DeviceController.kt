package com.reborn.server.domain.device.controller

import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.service.DeviceService
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.ApiResponse
import com.reborn.server.global.model.CommonErrorCode
import jakarta.validation.Valid
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
        @Valid @RequestBody request: DeviceDto.RegisterRequest,
        authentication: Authentication,
    ): ApiResponse<DeviceDto.RegisterResponse> {
        val userId = authentication.principal as? Long
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "인증 정보가 유효하지 않습니다.")
        return ApiResponse.success(deviceService.register(userId, request))
    }
}
