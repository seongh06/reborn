package com.reborn.core.data.mapper

import com.reborn.core.model.Device
import com.reborn.core.model.PairedDevice
import com.reborn.core.model.PairingCode
import com.reborn.core.network.model.response.device.DeviceItemResponse
import com.reborn.core.network.model.response.device.PairingCodeResponse
import com.reborn.core.network.model.response.device.PairingResponse

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
    )
