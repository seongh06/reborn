package com.reborn.core.network.model.request.device

import kotlinx.serialization.Serializable

// 서버 DeviceDto.AutoControlRuleRequest(#190)와 필드명을 그대로 맞춤 - 임계값/액션은 클라이언트
// 프리셋 문구("28°C", "냉방 시작" 등)를 그대로 저장하는 자유 텍스트라 서버는 파싱만 담당한다.
@Serializable
data class AutoControlRuleRequest(
    val discomfortThreshold: String? = null,
    val discomfortAction: String? = null,
    val humidityHighThreshold: String? = null,
    val humidityHighAction: String? = null,
    val humidityLowThreshold: String? = null,
    val humidityLowAction: String? = null,
    val temperatureHighThreshold: String? = null,
    val temperatureHighAction: String? = null,
    val temperatureLowThreshold: String? = null,
    val temperatureLowAction: String? = null,
    val occupancyThreshold: String? = null,
    val occupancyAction: String? = null,
    val isAutoOffEnabled: Boolean = false,
    val autoOffMinutes: String? = null,
)
