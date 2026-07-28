package com.reborn.server.domain.smartthings.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

class SmartThingsDto {

    data class AuthorizeResponse(
        val authorizeUrl: String,
    )

    data class DeviceSummary(
        val deviceId: String,
        val label: String?,
    )

    data class DeviceListResponse(
        val devices: List<DeviceSummary>,
    )

    data class RegisterDeviceRequest(
        @field:NotNull val placeId: Long? = null,
        @field:NotBlank val smartThingsDeviceId: String? = null,
        @field:NotBlank val deviceName: String? = null,
        // 아이콘 구분용(LAMP/PLUG/TV/AIR_CONDITIONER/CURTAIN/OTHER) - 관리자가 등록 화면에서 직접 선택.
        // SmartThings API가 카테고리를 안정적으로 안 내려줘서 자동 추론 대신 수동 선택으로 결정(#166 QA).
        val category: String? = null,
    )
}
