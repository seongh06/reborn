package com.reborn.server.domain.device.repository

import com.reborn.server.domain.device.DeviceSerial
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceSerialRepository : JpaRepository<DeviceSerial, Long> {

    fun findBySerial(serial: String): DeviceSerial?
}
