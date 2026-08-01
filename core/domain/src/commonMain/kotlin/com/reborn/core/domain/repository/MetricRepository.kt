package com.reborn.core.domain.repository

import com.reborn.core.model.Metric

interface MetricRepository {
    // 공기계 전용(#294) - 재실인원/조도 측정값을 서버에 전송
    suspend fun collect(deviceId: String, illuminance: Int, peopleCount: Int): Result<Unit>

    suspend fun getCurrent(deviceId: String): Result<Metric>
}
