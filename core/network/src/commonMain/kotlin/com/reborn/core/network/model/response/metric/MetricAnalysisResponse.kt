package com.reborn.core.network.model.response.metric

import kotlinx.serialization.Serializable

@Serializable
data class MetricAnalysisResponse(
    val deviceId: String,
    val category: String,
    val analysisText: String,
)
