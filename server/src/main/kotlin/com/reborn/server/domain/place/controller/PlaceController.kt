package com.reborn.server.domain.place.controller

import com.reborn.server.domain.place.dto.PlaceDto
import com.reborn.server.domain.place.service.PlaceService
import com.reborn.server.global.model.ApiResponse
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
        @RequestBody request: PlaceDto.RegisterRequest,
        authentication: Authentication,
    ): ApiResponse<PlaceDto.RegisterResponse> =
        ApiResponse.success(placeService.register(authentication.principal as Long, request))
}
