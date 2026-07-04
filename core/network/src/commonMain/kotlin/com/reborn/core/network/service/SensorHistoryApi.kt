package com.reborn.core.network.service

import com.reborn.core.network.model.SensorHistoryResponse

// TODO: 서버 /api/data/history 연동 확정 후 Ktor HttpClient 기반 구현체로 대체 예정
interface SensorHistoryApi {
    suspend fun getSensorHistory(deviceId: Int, sensorType: String): SensorHistoryResponse
}
