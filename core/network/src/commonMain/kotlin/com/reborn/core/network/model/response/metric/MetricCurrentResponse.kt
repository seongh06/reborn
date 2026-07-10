package com.reborn.core.network.model.response.metric

import kotlinx.serialization.Serializable

@Serializable
data class MetricCurrentResponse(
    val deviceId: String,
    val deviceName: String?,
    val temperature: Double?,
    val humidity: Double?,
    val illuminance: Int?,
    val peopleCount: Int?,
    val discomfort: Double?,
    val createdAt: String,
)
