package com.reborn.server.domain.metric.service

import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.feedback.client.GeminiClient
import com.reborn.server.domain.metric.MetricAggregateProjection
import com.reborn.server.domain.metric.MetricLog
import com.reborn.server.domain.metric.MetricLogRepository
import com.reborn.server.domain.metric.converter.MetricConverter
import com.reborn.server.domain.metric.dto.MetricDto
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MetricService(
    private val deviceRepository: DeviceRepository,
    private val metricLogRepository: MetricLogRepository,
    private val userPlaceMappingRepository: UserPlaceMappingRepository,
    private val geminiClient: GeminiClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun collect(deviceId: String, request: MetricDto.CollectRequest): MetricDto.CollectResponse {
        validateCollectRequest(request)

        val device = deviceRepository.findByDeviceKey(deviceId)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "등록되지 않은 기기입니다.")

        val metricLog = metricLogRepository.save(
            MetricLog(
                device = device,
                temperature = request.temperature,
                humidity = request.humidity,
                illuminance = request.illuminance,
                occupancy = request.peopleCount,
            ),
        )
        device.updateOnlineStatus(true)

        return MetricConverter.toCollectResponse(metricLog)
    }

    fun getHistory(deviceId: String, userId: Long, pageable: Pageable): MetricDto.HistoryResponse {
        val device = deviceRepository.findByDeviceKey(deviceId)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "등록되지 않은 기기입니다.")

        if (!userPlaceMappingRepository.existsByUserIdAndPlaceId(userId, device.place.id)) {
            throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "해당 장소에 대한 접근 권한이 없습니다.")
        }

        val logs = metricLogRepository.findAllByDeviceId(device.id, pageable)
        return MetricConverter.toHistoryResponse(deviceId, logs)
    }

    fun getCurrent(deviceId: String): MetricDto.CurrentResponse {
        val device = deviceRepository.findByDeviceKey(deviceId)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "등록되지 않은 기기이거나 수집된 데이터가 없습니다.")

        val latestLog = metricLogRepository.findTopByDeviceIdOrderByCreatedAtDesc(device.id)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "등록되지 않은 기기이거나 수집된 데이터가 없습니다.")

        return MetricConverter.toCurrentResponse(device, latestLog)
    }

    // 주/월/년 장기 집계(데이터 화면 #158) — HOUR/DAY는 기존 getHistory(원시 로그) 기반 그대로 두고,
    // 장기 구간만 별도 네이티브 GROUP BY로 조회한다(원시 로그를 그대로 내려주면 연 단위는 시간당 값이
    // 수만 건이라 감당 불가). 클라이언트가 라벨을 독립적으로 생성하므로(자연스러운 개수만큼만 표시)
    // 버킷이 모자라도 문제없이 붙는다 - getOrNull 안전망은 클라이언트 chartLabelsFor 참고.
    fun getAggregate(deviceId: String, userId: Long, period: String): MetricDto.AggregateResponse {
        val device = deviceRepository.findByDeviceKey(deviceId)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "등록되지 않은 기기입니다.")

        if (!userPlaceMappingRepository.existsByUserIdAndPlaceId(userId, device.place.id)) {
            throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "해당 장소에 대한 접근 권한이 없습니다.")
        }

        val projections: List<MetricAggregateProjection> = when (period.uppercase()) {
            "WEEK" -> metricLogRepository.findWeeklyAggregates(device.id).takeLast(WEEK_BUCKET_COUNT)
            "MONTH" -> metricLogRepository.findMonthlyAggregates(device.id).takeLast(MONTH_BUCKET_COUNT)
            "YEAR" -> metricLogRepository.findYearlyAggregates(device.id).takeLast(YEAR_BUCKET_COUNT)
            else -> throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "지원하지 않는 집계 단위입니다.")
        }

        return MetricConverter.toAggregateResponse(deviceId, period.uppercase(), projections)
    }

    // AI 분석 텍스트(데이터 화면 #158) - 기간(period)과 무관하게 최신 측정값 1건을 기준으로 생성한다
    // (클라이언트 mockAnalysisText도 원래 카테고리에만 의존, 기간엔 무관했던 것과 동일한 동작).
    fun getAnalysis(deviceId: String, userId: Long, category: String): MetricDto.AnalysisResponse {
        val device = deviceRepository.findByDeviceKey(deviceId)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "등록되지 않은 기기입니다.")

        if (!userPlaceMappingRepository.existsByUserIdAndPlaceId(userId, device.place.id)) {
            throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "해당 장소에 대한 접근 권한이 없습니다.")
        }

        val normalizedCategory = category.uppercase()
        val latest = metricLogRepository.findTopByDeviceIdOrderByCreatedAtDesc(device.id)
            ?: return MetricDto.AnalysisResponse(
                deviceId = deviceId,
                category = normalizedCategory,
                analysisText = "아직 수집된 데이터가 없어 분석할 수 없습니다.",
            )

        val analysisText = runCatching { geminiClient.generateText(buildAnalysisPrompt(normalizedCategory, latest)) }
            .onFailure { e -> log.warn("데이터 화면 AI 분석 생성 실패: deviceId={}, error={}", deviceId, e.message) }
            .getOrElse { "지금은 분석을 생성할 수 없습니다. 잠시 후 다시 시도해주세요." }

        return MetricDto.AnalysisResponse(deviceId = deviceId, category = normalizedCategory, analysisText = analysisText)
    }

    private fun buildAnalysisPrompt(category: String, metricLog: MetricLog): String {
        val discomfort = MetricConverter.calculateDiscomfort(metricLog.temperature, metricLog.humidity)
        val focus = when (category) {
            "TEMPERATURE" -> "온도"
            "HUMIDITY" -> "습도"
            "ILLUMINANCE" -> "조도"
            "PEOPLE_COUNT" -> "재실 인원"
            "DISCOMFORT" -> "불쾌지수"
            else -> "실내 환경"
        }
        return """
            실내 환경 모니터링 서비스(ReBorn)의 관리자 대시보드에 표시할 짧은 분석 문구를 작성하세요.
            지금 초점을 맞출 지표는 "$focus"입니다.
            최근 측정값 - 온도: ${metricLog.temperature ?: "미측정"}°C, 습도: ${metricLog.humidity ?: "미측정"}%, 조도: ${metricLog.illuminance ?: "미측정"}lux, 재실 인원: ${metricLog.occupancy ?: "미측정"}명, 불쾌지수: ${discomfort ?: "미측정"}
            위 값을 바탕으로 "$focus" 위주의 실내 환경 상태를 짚어주고, 필요하면 조치를 제안하는 한국어 1~2문장을 작성하세요.
            문장만 답하고 다른 설명이나 따옴표는 붙이지 마세요.
        """.trimIndent()
    }

    private fun validateCollectRequest(request: MetricDto.CollectRequest) {
        if (request.temperature == null && request.humidity == null && request.illuminance == null && request.peopleCount == null) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "수집된 센서 데이터가 없습니다.")
        }
        if (request.humidity != null && request.humidity !in 0.0..100.0) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "습도는 0~100 사이의 값이어야 합니다.")
        }
        if (request.illuminance != null && request.illuminance < 0) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "조도는 0 이상의 값이어야 합니다.")
        }
        if (request.peopleCount != null && request.peopleCount < 0) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "재실 인원은 0 이상의 값이어야 합니다.")
        }
    }

    companion object {
        // 클라이언트 AdminDataViewModel의 weeklyMockValues(8)/monthlyMockValues(12)/yearlyMockValues(5)와
        // 동일한 버킷 개수 - 라벨 생성 로직(chartLabelsFor)과 자리 수를 맞추기 위함.
        private const val WEEK_BUCKET_COUNT = 8
        private const val MONTH_BUCKET_COUNT = 12
        private const val YEAR_BUCKET_COUNT = 5
    }
}
