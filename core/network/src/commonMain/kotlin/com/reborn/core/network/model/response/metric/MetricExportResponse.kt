package com.reborn.core.network.model.response.metric

import kotlinx.serialization.Serializable

@Serializable
data class MetricExportResponse(
    val spreadsheetUrl: String,
)
