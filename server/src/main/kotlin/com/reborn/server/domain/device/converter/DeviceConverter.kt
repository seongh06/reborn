package com.reborn.server.domain.device.converter

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
}
