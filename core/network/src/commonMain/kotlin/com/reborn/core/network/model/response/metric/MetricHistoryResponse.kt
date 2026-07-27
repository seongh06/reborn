package com.reborn.core.network.model.response.metric

import kotlinx.serialization.Serializable

@Serializable
data class MetricHistoryResponse(
    val deviceId: String,
    val logs: List<MetricHistoryItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@Serializable
data class MetricHistoryItem(
    val logId: Long,
    val temperature: Double? = null,
    val humidity: Double? = null,
    val illuminance: Int? = null,
    val peopleCount: Int? = null,
    val createdAt: String,
)
