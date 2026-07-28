package com.reborn.server.domain.metric.dto

import java.time.LocalDateTime

class MetricDto {

    data class CollectRequest(
        val temperature: Double? = null,
        val humidity: Double? = null,
        val illuminance: Int? = null,
        val peopleCount: Int? = null,
    )

    data class CollectResponse(
        val logId: Long,
        val discomfort: Double?,
        val createdAt: LocalDateTime,
    )

    data class CurrentResponse(
        val deviceId: String,
        val deviceName: String?,
        val temperature: Double?,
        val humidity: Double?,
        val illuminance: Int?,
        val peopleCount: Int?,
        val discomfort: Double?,
        val createdAt: LocalDateTime,
    )

    data class HistoryResponse(
        val deviceId: String,
        val logs: List<HistoryItem>,
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    )

    data class HistoryItem(
        val logId: Long,
        val temperature: Double?,
        val humidity: Double?,
        val illuminance: Int?,
        val peopleCount: Int?,
        val createdAt: LocalDateTime,
    )

    data class AggregateResponse(
        val deviceId: String,
        val period: String,
        val buckets: List<AggregateBucket>,
    )

    data class AggregateBucket(
        val temperature: Double?,
        val humidity: Double?,
        val illuminance: Double?,
        val peopleCount: Double?,
        val discomfort: Double?,
    )

    data class AnalysisResponse(
        val deviceId: String,
        val category: String,
        val analysisText: String,
    )

    data class ExportResponse(
        val spreadsheetUrl: String,
    )
}
