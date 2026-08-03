package com.reborn.server.domain.device.service

import com.reborn.server.domain.device.DeviceScheduleRule
import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.repository.DeviceScheduleRuleRepository
import com.reborn.server.domain.smartthings.service.SmartThingsDeviceService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

// 시간 기반 자동제어 규칙 1건을 실제로 평가·실행한다(#325). SmartThings 기기만 대상 - 서버가
// 자격증명을 직접 보유해 동기 호출 가능하다(ARDUINO IR은 폴링 기반이라 "정확히 이 분에 실행"을
// 보장할 수 없어 스코프 밖).
@Service
class DeviceScheduleEvaluationService(
    private val deviceScheduleRuleRepository: DeviceScheduleRuleRepository,
    private val smartThingsDeviceService: SmartThingsDeviceService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun evaluateAndApply(rule: DeviceScheduleRule, now: LocalDateTime) {
        if (!rule.matches(now) || rule.alreadyTriggeredThisMinute(now)) {
            return
        }

        val device = rule.device
        val request = DeviceDto.ControlRequest(isPowerOn = rule.isPowerOn, operationMode = rule.operationMode)
        smartThingsDeviceService.controlInternal(device, request)

        rule.lastTriggeredAt = now
        deviceScheduleRuleRepository.save(rule)
        log.info("시간 기반 자동제어 실행: deviceId={}, hour={}, minute={}, isPowerOn={}", device.deviceKey, rule.hour, rule.minute, rule.isPowerOn)
    }
}
