package com.reborn.server.domain.device.converter

import com.reborn.server.domain.device.AutoControlRule
import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.dto.DeviceDto

object DeviceConverter {

    fun toRegisterResponse(entity: Device): DeviceDto.RegisterResponse =
        DeviceDto.RegisterResponse(
            deviceId = entity.deviceKey,
            deviceName = entity.name,
            deviceType = entity.deviceType.name,
            createdAt = requireNotNull(entity.createdAt),
        )

    fun toDeviceItem(entity: Device): DeviceDto.DeviceItem =
        DeviceDto.DeviceItem(
            deviceId = entity.deviceKey,
            deviceName = entity.name,
            deviceType = entity.deviceType.name,
            isOnline = entity.isOnline,
            createdAt = requireNotNull(entity.createdAt),
        )

    fun toAutoControlRuleResponse(entity: AutoControlRule): DeviceDto.AutoControlRuleResponse =
        DeviceDto.AutoControlRuleResponse(
            discomfortThreshold = entity.discomfortThreshold,
            discomfortAction = entity.discomfortAction,
            humidityHighThreshold = entity.humidityHighThreshold,
            humidityHighAction = entity.humidityHighAction,
            humidityLowThreshold = entity.humidityLowThreshold,
            humidityLowAction = entity.humidityLowAction,
            temperatureHighThreshold = entity.temperatureHighThreshold,
            temperatureHighAction = entity.temperatureHighAction,
            temperatureLowThreshold = entity.temperatureLowThreshold,
            temperatureLowAction = entity.temperatureLowAction,
            occupancyThreshold = entity.occupancyThreshold,
            occupancyAction = entity.occupancyAction,
            isAutoOffEnabled = entity.isAutoOffEnabled,
            autoOffMinutes = entity.autoOffMinutes,
        )
}
