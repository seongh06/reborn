package com.reborn.server.domain.device.service

import com.reborn.server.domain.device.repository.DeviceScheduleRuleRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

// 시간 기반 자동제어 규칙 평가 주기 트리거(#325). 매분 정각에 돌아 "지금 이 분에 실행될 규칙"이
// 있는지 훑는다 - AutoControlScheduler(#190, 기본 10분)보다 훨씬 촘촘한 주기가 필요해(시:분 단위로
// 정확히 맞아야 함) 별도 스케줄러로 분리했다. 실제 평가/실행은 DeviceScheduleEvaluationService에
// 위임(다른 빈이어야 @Transactional 프록시가 규칙별로 정상 적용됨, self-invocation 방지).
@Component
class DeviceScheduleScheduler(
    private val deviceScheduleRuleRepository: DeviceScheduleRuleRepository,
    private val deviceScheduleEvaluationService: DeviceScheduleEvaluationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 * * * * *")
    fun evaluate() {
        val now = LocalDateTime.now()
        val rules = deviceScheduleRuleRepository.findAllByEnabledTrue()
        rules.forEach { rule ->
            runCatching { deviceScheduleEvaluationService.evaluateAndApply(rule, now) }
                .onFailure { e -> log.warn("시간 기반 자동제어 평가 실패: deviceId={}, error={}", rule.device.deviceKey, e.message) }
        }
    }
}
