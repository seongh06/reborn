package com.reborn.core.network.model.request.device

import kotlinx.serialization.Serializable

// 서버 DeviceDto.ControlRequest(#132)와 필드명/enum 이름을 그대로 맞춤 - 보낸 필드만 SmartThings
// 커맨드로 매핑되므로 전부 optional.
@Serializable
data class ControlDeviceRequest(
    val isPowerOn: Boolean? = null,
    val operationMode: String? = null,
    val windSpeed: String? = null,
    val temperature: Int? = null,
)
