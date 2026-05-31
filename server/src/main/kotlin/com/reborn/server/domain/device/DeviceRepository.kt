package com.reborn.server.domain.device

import org.springframework.data.jpa.repository.JpaRepository

interface DeviceRepository : JpaRepository<Device, Long> {

    fun findByDeviceKey(deviceKey: String): Device?

    fun findAllByPlaceId(placeId: Long): List<Device>

    fun findAllByPlaceIdAndDeviceType(placeId: Long, deviceType: DeviceType): List<Device>

    fun existsByDeviceKey(deviceKey: String): Boolean
}
