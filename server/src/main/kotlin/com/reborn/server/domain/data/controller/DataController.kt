package com.reborn.server.domain.data.controller

import com.reborn.server.domain.data.dto.SensorDataDto
import com.reborn.server.domain.data.service.SensorDataService
import com.reborn.server.global.model.ApiResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/data")
class DataController(
    private val sensorDataService: SensorDataService,
) {

    @GetMapping("/history")
    fun getHistory(
        @RequestParam deviceId: String,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
        authentication: Authentication,
    ): ApiResponse<SensorDataDto.HistoryResponse> =
        ApiResponse.success(sensorDataService.getHistory(deviceId, authentication.principal as Long, pageable))
}
