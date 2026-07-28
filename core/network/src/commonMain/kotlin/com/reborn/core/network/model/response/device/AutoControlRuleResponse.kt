package com.reborn.core.network.model.response.device

import kotlinx.serialization.Serializable

@Serializable
data class AutoControlRuleResponse(
    val discomfortThreshold: String?,
    val discomfortAction: String?,
    val humidityHighThreshold: String?,
    val humidityHighAction: String?,
    val humidityLowThreshold: String?,
    val humidityLowAction: String?,
    val temperatureHighThreshold: String?,
    val temperatureHighAction: String?,
    val temperatureLowThreshold: String?,
    val temperatureLowAction: String?,
    val occupancyThreshold: String?,
    val occupancyAction: String?,
    val isAutoOffEnabled: Boolean,
    val autoOffMinutes: String?,
)
