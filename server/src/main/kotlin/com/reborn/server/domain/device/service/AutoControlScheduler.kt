package com.reborn.server.domain.device.service

import com.reborn.server.domain.device.repository.AutoControlRuleRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

// #190 — 자동 제어 규칙 평가 주기 트리거. 실제 평가/실행은 AutoControlEvaluationService에
// 위임한다(다른 빈이어야 @Transactional 프록시가 규칙별로 정상 적용됨, self-invocation 방지).
// 온도 상/하한 조건이 저장된 규칙만 대상 — 나머지 조건(습도/재실인원/불쾌지수/자동 꺼짐)은 미실행.
@Component
class AutoControlScheduler(
    private val autoControlRuleRepository: AutoControlRuleRepository,
    private val autoControlEvaluationService: AutoControlEvaluationService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 기본 10분 — SmartThings 온습도 폴링 주기(#133)와 맞춰 최신 값을 바로 평가할 수 있게 함.
    @Scheduled(fixedDelayString = "\${autocontrol.evaluation-interval-ms:600000}")
    fun evaluate() {
        val rules = autoControlRuleRepository
            .findAllByTemperatureHighThresholdIsNotNullOrTemperatureLowThresholdIsNotNull()
        rules.forEach { rule ->
            runCatching { autoControlEvaluationService.evaluateAndApply(rule) }
                .onFailure { e -> log.warn("자동 제어 평가 실패: deviceId={}, error={}", rule.device.deviceKey, e.message) }
        }
    }
}
