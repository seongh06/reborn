package com.reborn.core.network.model.response.device

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleRuleResponse(
    val id: Long,
    val deviceId: String,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: List<String>,
    val isPowerOn: Boolean,
    val operationMode: String?,
    val enabled: Boolean,
)

@Serializable
data class ScheduleRuleListResponse(
    val rules: List<ScheduleRuleResponse>,
)
