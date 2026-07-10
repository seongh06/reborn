package com.reborn.core.network.model.response.metric

import kotlinx.serialization.Serializable

@Serializable
data class MetricHistoryResponse(
    val deviceId: String,
    val logs: List<MetricHistoryItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@Serializable
data class MetricHistoryItemResponse(
    val logId: Long,
    val temperature: Double?,
    val humidity: Double?,
    val illuminance: Int?,
    val peopleCount: Int?,
    val createdAt: String,
)
