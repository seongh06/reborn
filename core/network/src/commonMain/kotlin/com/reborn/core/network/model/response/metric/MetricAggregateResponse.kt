package com.reborn.core.network.model.response.metric

import kotlinx.serialization.Serializable

@Serializable
data class MetricAggregateResponse(
    val deviceId: String,
    val period: String,
    val buckets: List<MetricAggregateBucket>,
)

@Serializable
data class MetricAggregateBucket(
    val temperature: Double? = null,
    val humidity: Double? = null,
    val illuminance: Double? = null,
    val peopleCount: Double? = null,
    val discomfort: Double? = null,
)
