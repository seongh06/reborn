package com.reborn.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class SensorHistoryResponse(
    val deviceId: Int,
    val sensorType: String,
    val dailyData: Map<String, List<Double>> // Key: "yyyyMMdd", Value: 0시부터 1시간 단위로 정렬된 값 배열
)
