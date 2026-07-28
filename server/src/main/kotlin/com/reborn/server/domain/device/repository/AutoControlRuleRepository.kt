package com.reborn.server.domain.device.repository

import com.reborn.server.domain.device.AutoControlRule
import org.springframework.data.jpa.repository.JpaRepository

interface AutoControlRuleRepository : JpaRepository<AutoControlRule, Long> {

    fun findByDeviceId(deviceId: Long): AutoControlRule?

    // 온도 조건이 하나라도 설정된 규칙만 스케줄러가 훑도록 - 전부 null인 신규 저장 없는 규칙은 제외
    fun findAllByTemperatureHighThresholdIsNotNullOrTemperatureLowThresholdIsNotNull(): List<AutoControlRule>
}
