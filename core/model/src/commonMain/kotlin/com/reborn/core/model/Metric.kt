package com.reborn.core.model

data class MetricSnapshot(
    val deviceId: String,
    val deviceName: String?,
    val temperature: Double?,
    val humidity: Double?,
    val illuminance: Int?,
    val peopleCount: Int?,
    val discomfort: Double?,
    val createdAt: String,
)

data class MetricLog(
    val logId: Long,
    val temperature: Double?,
    val humidity: Double?,
    val illuminance: Int?,
    val peopleCount: Int?,
    val createdAt: String,
)

data class MetricHistoryPage(
    val deviceId: String,
    val logs: List<MetricLog>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
