package com.reborn.core.network.service

import com.reborn.core.network.datasource.MetricDataSource
import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.SensorHistoryResponse
import com.reborn.core.network.model.response.metric.MetricHistoryItem
import kotlinx.datetime.LocalDateTime

// 최근 조회 페이지(size)만큼의 로그를 날짜(yyyyMMdd)+시간(0~23)별로 묶어 시간당 평균값으로 변환.
// 서버 히스토리 API는 날짜 범위 필터가 없어 "최근 N건"으로만 근사 - 수집 주기가 촘촘한 기기는
// size를 늘려야 최근 며칠을 온전히 커버할 수 있음(HISTORY_PAGE_SIZE 조정 필요할 수 있음).
class SensorHistoryApiImpl(
    private val metricDataSource: MetricDataSource,
) : SensorHistoryApi {

    override suspend fun getSensorHistory(deviceId: String, sensorType: String): SensorHistoryResponse {
        val response = metricDataSource.getHistory(deviceId, HISTORY_PAGE_SIZE)
        val logs = when (response) {
            is ApiResponse.Success -> response.data.logs
            is ApiResponse.Failure.HttpError -> throw IllegalStateException(response.message)
            is ApiResponse.Failure.NetworkError -> throw IllegalStateException(response.message)
            is ApiResponse.Failure.UnknownApiError -> throw IllegalStateException(response.message)
        }

        val extractor = fieldExtractorFor(sensorType)
        val dailyData = logs
            .mapNotNull { item -> extractor(item)?.let { value -> item.dateKey() to (item.hour() to value) } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, hourValues) ->
                (0..23).map { hour ->
                    hourValues.filter { it.first == hour }.map { it.second }
                        .let { values -> if (values.isEmpty()) 0.0 else values.average() }
                }
            }

        return SensorHistoryResponse(deviceId = deviceId, sensorType = sensorType, dailyData = dailyData)
    }

    private fun fieldExtractorFor(sensorType: String): (MetricHistoryItem) -> Double? = when (sensorType) {
        "TEMPERATURE" -> { item -> item.temperature }
        "HUMIDITY" -> { item -> item.humidity }
        "ILLUMINANCE" -> { item -> item.illuminance?.toDouble() }
        "PEOPLE_COUNT" -> { item -> item.peopleCount?.toDouble() }
        else -> { _ -> null }
    }

    private fun MetricHistoryItem.dateKey(): String {
        val dt = LocalDateTime.parse(createdAt)
        return "${dt.year}${dt.monthNumber.pad2()}${dt.dayOfMonth.pad2()}"
    }

    private fun MetricHistoryItem.hour(): Int = LocalDateTime.parse(createdAt).hour

    private fun Int.pad2(): String = toString().padStart(2, '0')

    companion object {
        private const val HISTORY_PAGE_SIZE = 200
    }
}
