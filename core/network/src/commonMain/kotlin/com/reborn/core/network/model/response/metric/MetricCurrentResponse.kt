package com.reborn.core.network.model.response.metric

import kotlinx.serialization.Serializable

@Serializable
data class MetricCurrentResponse(
    val deviceId: String,
    val deviceName: String? = null,
    val temperature: Double? = null,
    val humidity: Double? = null,
    val illuminance: Int? = null,
    val peopleCount: Int? = null,
    val discomfort: Double? = null,
    val createdAt: String,
)
