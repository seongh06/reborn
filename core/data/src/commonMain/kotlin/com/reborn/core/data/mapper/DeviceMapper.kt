package com.reborn.core.data.mapper

import com.reborn.core.model.AutoControlRule
import com.reborn.core.model.Device
import com.reborn.core.model.DeviceStatus
import com.reborn.core.model.PairedDevice
import com.reborn.core.model.PairingCode
import com.reborn.core.model.RegisteredDevice
import com.reborn.core.network.model.response.device.AutoControlRuleResponse
import com.reborn.core.network.model.response.device.DeviceItemResponse
import com.reborn.core.network.model.response.device.DeviceStatusResponse
import com.reborn.core.network.model.response.device.PairingCodeResponse
import com.reborn.core.network.model.response.device.PairingResponse
import com.reborn.core.network.model.response.device.RegisterDeviceResponse

fun PairingCodeResponse.toPairingCode(): PairingCode =
    PairingCode(code = pairingCode, expiresAt = expiresAt)

fun PairingResponse.toPairedDevice(): PairedDevice =
    PairedDevice(deviceId = deviceId, placeId = placeId, appToken = appToken)

fun DeviceItemResponse.toDevice(): Device =
    Device(
        deviceId = deviceId,
        deviceName = deviceName,
        deviceType = deviceType,
        isOnline = isOnline,
        createdAt = createdAt,
        category = category,
        hasIrControl = hasIrControl,
    )

fun RegisterDeviceResponse.toRegisteredDevice(): RegisteredDevice =
    RegisteredDevice(
        deviceId = deviceId,
        deviceName = deviceName,
        deviceType = deviceType,
        createdAt = createdAt,
        category = category,
    )

fun DeviceStatusResponse.toDeviceStatus(): DeviceStatus =
    DeviceStatus(
        isPowerOn = isPowerOn,
        operationMode = operationMode,
        windSpeed = windSpeed,
        temperature = temperature,
    )

fun AutoControlRuleResponse.toAutoControlRule(): AutoControlRule =
    AutoControlRule(
        discomfortThreshold = discomfortThreshold,
        discomfortAction = discomfortAction,
        humidityHighThreshold = humidityHighThreshold,
        humidityHighAction = humidityHighAction,
        humidityLowThreshold = humidityLowThreshold,
        humidityLowAction = humidityLowAction,
        temperatureHighThreshold = temperatureHighThreshold,
        temperatureHighAction = temperatureHighAction,
        temperatureLowThreshold = temperatureLowThreshold,
        temperatureLowAction = temperatureLowAction,
        occupancyThreshold = occupancyThreshold,
        occupancyAction = occupancyAction,
        isAutoOffEnabled = isAutoOffEnabled,
        autoOffMinutes = autoOffMinutes,
    )
