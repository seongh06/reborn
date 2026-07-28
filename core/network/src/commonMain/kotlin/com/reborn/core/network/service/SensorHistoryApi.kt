package com.reborn.core.network.service

import com.reborn.core.network.model.SensorHistoryResponse

interface SensorHistoryApi {
    suspend fun getSensorHistory(deviceId: String, sensorType: String): SensorHistoryResponse

    // 주/월/년 장기 집계 - 서버가 이미 기간 단위로 평균 낸 값을 그대로 순서대로 반환(오래된 것 -> 최신)
    suspend fun getSensorAggregate(deviceId: String, sensorType: String, period: String): List<Double?>

    suspend fun getAnalysisText(deviceId: String, sensorType: String): String

    suspend fun exportToSheets(deviceId: String): String
}
