package com.reborn.server.domain.data.controller

import com.reborn.server.domain.data.dto.SensorDataDto
import com.reborn.server.domain.data.service.SensorDataService
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.ApiResponse
import com.reborn.server.global.model.CommonErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "센서 데이터 히스토리 API", description = "장소별 센서 로그 이력 조회 (인증 필요)")
@RestController
@RequestMapping("/api/data")
class DataController(
    private val sensorDataService: SensorDataService,
) {

    @Operation(summary = "센서 로그 히스토리 조회", description = "특정 기기의 센서 로그 목록을 페이징하여 반환합니다. 해당 장소에 접근 권한이 있는 사용자만 조회 가능합니다.")
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공 — 로그 목록 및 페이징 정보 반환"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않거나 만료된 AccessToken)"),
        SwaggerApiResponse(responseCode = "403", description = "해당 장소에 대한 접근 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "등록되지 않은 기기 ID"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/history")
    fun getHistory(
        @RequestParam deviceId: String,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
        authentication: Authentication,
    ): ApiResponse<SensorDataDto.HistoryResponse> {
        val userId = authentication.principal as? Long
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "인증 정보가 유효하지 않습니다.")
        return ApiResponse.success(sensorDataService.getHistory(deviceId, userId, pageable))
    }
}
