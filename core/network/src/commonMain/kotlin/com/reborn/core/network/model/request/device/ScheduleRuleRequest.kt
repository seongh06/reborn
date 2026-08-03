package com.reborn.core.network.model.request.device

import kotlinx.serialization.Serializable

// 서버 DeviceDto.ScheduleRuleRequest(#325)와 필드명을 그대로 맞춤.
@Serializable
data class ScheduleRuleRequest(
    val hour: Int,
    val minute: Int,
    val daysOfWeek: List<String>,
    val isPowerOn: Boolean,
    val operationMode: String? = null,
)

@Serializable
data class ScheduleRuleUpdateRequest(
    val enabled: Boolean,
)
