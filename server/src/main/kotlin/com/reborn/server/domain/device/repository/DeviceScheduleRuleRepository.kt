package com.reborn.server.domain.device.repository

import com.reborn.server.domain.device.DeviceScheduleRule
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceScheduleRuleRepository : JpaRepository<DeviceScheduleRule, Long> {

    fun findAllByDeviceId(deviceId: Long): List<DeviceScheduleRule>

    // 스케줄러가 매분 훑는 대상 - 꺼둔 규칙은 애초에 안 걸러도 되지만 인덱스(enabled, hour, minute)를
    // 그대로 타게 조건에 명시한다.
    fun findAllByEnabledTrue(): List<DeviceScheduleRule>
}
