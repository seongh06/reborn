package com.reborn.server.domain.metric.controller

import com.reborn.server.domain.metric.dto.MetricDto
import com.reborn.server.domain.metric.service.MetricExportService
import com.reborn.server.domain.metric.service.MetricService
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "메트릭 API", description = "실내 환경 지표(온습도·조도·재실 인원) 수집 및 조회")
@RestController
@RequestMapping("/api/metric")
class MetricController(
    private val metricService: MetricService,
    private val metricExportService: MetricExportService,
) {

    @Operation(
        summary = "메트릭 수집",
        description = "Arduino 기기가 온도·습도·조도·재실 인원 데이터를 서버에 전송합니다. 헤더의 X-Device-Id로 기기를 식별합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "수집 성공 — logId, 불쾌지수, 수집일시 반환"),
        SwaggerApiResponse(responseCode = "400", description = "센서 값이 모두 비어있거나 유효 범위를 벗어난 경우"),
        SwaggerApiResponse(responseCode = "404", description = "등록되지 않은 기기 ID"),
    )
    @PostMapping("/collect")
    fun collect(
        @RequestHeader("X-Device-Id") deviceId: String,
        @RequestBody request: MetricDto.CollectRequest,
    ): ApiResponse<MetricDto.CollectResponse> =
        ApiResponse.success(metricService.collect(deviceId, request))

    @Operation(
        summary = "최신 메트릭 조회",
        description = "특정 기기의 가장 최근 메트릭 로그 1건을 조회합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공 — 온도·습도·조도·재실 인원·불쾌지수 반환"),
        SwaggerApiResponse(responseCode = "404", description = "등록되지 않은 기기이거나 수집된 데이터 없음"),
    )
    @GetMapping("/current")
    fun getCurrent(
        @RequestParam deviceId: String,
    ): ApiResponse<MetricDto.CurrentResponse> =
        ApiResponse.success(metricService.getCurrent(deviceId))

    @Operation(
        summary = "메트릭 히스토리 조회",
        description = "특정 기기의 메트릭 로그 목록을 페이징하여 반환합니다. 해당 장소에 접근 권한이 있는 사용자만 조회 가능합니다.",
    )
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
    ): ApiResponse<MetricDto.HistoryResponse> {
        val userId = authentication.principal as? Long
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "인증 정보가 유효하지 않습니다.")
        return ApiResponse.success(metricService.getHistory(deviceId, userId, pageable))
    }

    @Operation(
        summary = "메트릭 주/월/년 집계 조회",
        description = "특정 기기의 메트릭을 주/월/년 단위로 평균 집계하여 반환합니다(데이터 화면 장기 차트용).",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공 — 기간별 평균값 버킷 목록 반환"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        SwaggerApiResponse(responseCode = "403", description = "해당 장소에 대한 접근 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "등록되지 않은 기기 ID"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/aggregate")
    fun getAggregate(
        @RequestParam deviceId: String,
        @RequestParam period: String,
        authentication: Authentication,
    ): ApiResponse<MetricDto.AggregateResponse> {
        val userId = extractUserId(authentication)
        return ApiResponse.success(metricService.getAggregate(deviceId, userId, period))
    }

    @Operation(
        summary = "AI 분석 텍스트 조회",
        description = "특정 기기의 최신 측정값을 바탕으로 Gemini가 생성한 짧은 한국어 분석 문구를 반환합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        SwaggerApiResponse(responseCode = "403", description = "해당 장소에 대한 접근 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "등록되지 않은 기기 ID"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/analysis")
    fun getAnalysis(
        @RequestParam deviceId: String,
        @RequestParam category: String,
        authentication: Authentication,
    ): ApiResponse<MetricDto.AnalysisResponse> {
        val userId = extractUserId(authentication)
        return ApiResponse.success(metricService.getAnalysis(deviceId, userId, category))
    }

    @Operation(
        summary = "Google Sheets로 데이터 내보내기",
        description = "특정 기기의 최근 측정 이력 전체를 새 Google Sheets 문서로 내보냅니다. " +
            "내보내기 전 해당 장소가 Google Sheets 계정과 연동되어 있어야 합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "내보내기 성공 — 생성된 스프레드시트 URL 반환"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        SwaggerApiResponse(responseCode = "403", description = "해당 장소에 대한 접근 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "등록되지 않은 기기 ID이거나 Google Sheets 미연동"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/export")
    fun export(
        @RequestParam deviceId: String,
        authentication: Authentication,
    ): ApiResponse<MetricDto.ExportResponse> {
        val userId = extractUserId(authentication)
        return ApiResponse.success(metricExportService.exportToSheets(deviceId, userId))
    }

    private fun extractUserId(authentication: Authentication): Long =
        authentication.principal as? Long
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "인증 정보가 유효하지 않습니다.")
}
