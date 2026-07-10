package com.reborn.core.data.mapper

import com.reborn.core.model.MetricHistoryPage
import com.reborn.core.model.MetricLog
import com.reborn.core.model.MetricSnapshot
import com.reborn.core.network.model.response.metric.MetricCurrentResponse
import com.reborn.core.network.model.response.metric.MetricHistoryItemResponse
import com.reborn.core.network.model.response.metric.MetricHistoryResponse

fun MetricCurrentResponse.toMetricSnapshot(): MetricSnapshot =
    MetricSnapshot(
        deviceId = deviceId,
        deviceName = deviceName,
        temperature = temperature,
        humidity = humidity,
        illuminance = illuminance,
        peopleCount = peopleCount,
        discomfort = discomfort,
        createdAt = createdAt,
    )

fun MetricHistoryItemResponse.toMetricLog(): MetricLog =
    MetricLog(
        logId = logId,
        temperature = temperature,
        humidity = humidity,
        illuminance = illuminance,
        peopleCount = peopleCount,
        createdAt = createdAt,
    )

fun MetricHistoryResponse.toMetricHistoryPage(): MetricHistoryPage =
    MetricHistoryPage(
        deviceId = deviceId,
        logs = logs.map { it.toMetricLog() },
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )
