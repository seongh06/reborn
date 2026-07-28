package com.reborn.server.domain.metric.service

import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.googlesheets.client.GoogleSheetsWriterClient
import com.reborn.server.domain.googlesheets.service.GoogleSheetsService
import com.reborn.server.domain.metric.MetricLog
import com.reborn.server.domain.metric.MetricLogRepository
import com.reborn.server.domain.metric.converter.MetricConverter
import com.reborn.server.domain.metric.dto.MetricDto
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// 데이터 화면 "Google Sheets로 내보내기" - 특정 카테고리/기간이 아니라 그 기기의 최근 측정 이력
// 전체(온도/습도/조도/재실인원/불쾌지수)를 한 시트에 내보낸다. 화면에 보이는 차트 한 조각만 내보내는
// 것보다 실제로 쓸모 있는 원본 데이터 형태라고 판단해 이렇게 범위를 잡음.
@Service
@Transactional(readOnly = true)
class MetricExportService(
    private val deviceRepository: DeviceRepository,
    private val metricLogRepository: MetricLogRepository,
    private val userPlaceMappingRepository: UserPlaceMappingRepository,
    private val googleSheetsService: GoogleSheetsService,
    private val googleSheetsWriterClient: GoogleSheetsWriterClient,
) {

    fun exportToSheets(deviceId: String, userId: Long): MetricDto.ExportResponse {
        val device = deviceRepository.findByDeviceKey(deviceId)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "등록되지 않은 기기입니다.")

        if (!userPlaceMappingRepository.existsByUserIdAndPlaceId(userId, device.place.id)) {
            throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "해당 장소에 대한 접근 권한이 없습니다.")
        }

        val accessToken = googleSheetsService.getValidAccessToken(device.place.id)

        val logs = metricLogRepository
            .findAllByDeviceId(device.id, PageRequest.of(0, EXPORT_ROW_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt")))
            .content
            .sortedBy { it.createdAt }

        val title = "ReBorn 데이터 내보내기 - ${device.name ?: device.deviceKey} - ${LocalDateTime.now().format(TITLE_FORMATTER)}"
        val spreadsheetId = googleSheetsWriterClient.createSpreadsheet(accessToken, title)
        googleSheetsWriterClient.writeValues(accessToken, spreadsheetId, "A1", buildRows(logs))

        return MetricDto.ExportResponse(spreadsheetUrl = "https://docs.google.com/spreadsheets/d/$spreadsheetId/edit")
    }

    private fun buildRows(logs: List<MetricLog>): List<List<Any?>> {
        val header = listOf("측정 시각", "온도(°C)", "습도(%)", "조도(lux)", "재실 인원(명)", "불쾌지수")
        val rows = logs.map { log ->
            listOf(
                log.createdAt?.toString(),
                log.temperature,
                log.humidity,
                log.illuminance,
                log.occupancy,
                MetricConverter.calculateDiscomfort(log.temperature, log.humidity),
            )
        }
        return listOf(header) + rows
    }

    companion object {
        private const val EXPORT_ROW_LIMIT = 1000
        private val TITLE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
