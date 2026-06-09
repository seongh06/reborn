package com.reborn.server.domain.place.controller

import com.reborn.server.domain.place.dto.PlaceDto
import com.reborn.server.domain.place.service.PlaceService
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
@RequestMapping("/api/place")
class PlaceController(
    private val placeService: PlaceService,
) {

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
