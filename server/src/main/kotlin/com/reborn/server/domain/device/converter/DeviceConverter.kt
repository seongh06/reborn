package com.reborn.server.domain.device.converter

import com.reborn.server.domain.device.AutoControlRule
import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceScheduleRule
import com.reborn.server.domain.device.dto.DeviceDto

object DeviceConverter {

    fun toRegisterResponse(entity: Device): DeviceDto.RegisterResponse =
        DeviceDto.RegisterResponse(
            deviceId = entity.deviceKey,
            deviceName = entity.name,
            deviceType = entity.deviceType.name,
            category = entity.category,
            createdAt = requireNotNull(entity.createdAt),
        )

    fun toDeviceItem(entity: Device, isOnline: Boolean = entity.isOnline): DeviceDto.DeviceItem =
        DeviceDto.DeviceItem(
            deviceId = entity.deviceKey,
            deviceName = entity.name,
            deviceType = entity.deviceType.name,
            category = entity.category,
            isOnline = isOnline,
            hasIrControl = entity.hasIrControl,
            createdAt = requireNotNull(entity.createdAt),
        )

    fun toScheduleRuleResponse(entity: DeviceScheduleRule): DeviceDto.ScheduleRuleResponse =
        DeviceDto.ScheduleRuleResponse(
            id = entity.id,
            deviceId = entity.device.deviceKey,
            hour = entity.hour,
            minute = entity.minute,
            daysOfWeek = entity.daysOfWeek.map { it.name },
            isPowerOn = entity.isPowerOn,
            operationMode = entity.operationMode,
            enabled = entity.enabled,
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
