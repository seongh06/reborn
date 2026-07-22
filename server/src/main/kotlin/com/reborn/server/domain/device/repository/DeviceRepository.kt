package com.reborn.server.domain.device.repository

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceRepository : JpaRepository<Device, Long> {

    fun findByDeviceKey(deviceKey: String): Device?

    fun findAllByPlaceId(placeId: Long): List<Device>

    fun findAllByPlaceIdAndDeviceType(placeId: Long, deviceType: DeviceType): List<Device>

    fun findAllByDeviceType(deviceType: DeviceType): List<Device>

    fun countByPlaceId(placeId: Long): Long
}