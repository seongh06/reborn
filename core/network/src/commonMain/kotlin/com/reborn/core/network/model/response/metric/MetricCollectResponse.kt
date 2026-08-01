package com.reborn.core.network.model.response.metric

import kotlinx.serialization.Serializable

@Serializable
data class MetricCollectResponse(
    val logId: Long,
    val discomfort: Double? = null,
    val createdAt: String,
)
