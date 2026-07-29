package com.reborn.core.network.model.response.device

import kotlinx.serialization.Serializable

// 필드가 null이면 이 기기가 그 컨트롤을 지원하지 않는다는 뜻(#221) - 서버 DeviceDto.StatusResponse와 동일.
@Serializable
data class DeviceStatusResponse(
    val isPowerOn: Boolean? = null,
    val operationMode: String? = null,
    val windSpeed: String? = null,
    val temperature: Double? = null,
)
