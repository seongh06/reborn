package com.reborn.server.domain.smartthings.service

import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.repository.DeviceRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

// #133 — SmartThings 온습도 폴링 주기 트리거. 실제 조회/저장은 SmartThingsMetricPollingService에
// 위임한다(다른 빈이어야 @Transactional 프록시가 기기별로 정상 적용됨, self-invocation 방지).
@Component
class SmartThingsMetricPollingScheduler(
    private val deviceRepository: DeviceRepository,
    private val smartThingsMetricPollingService: SmartThingsMetricPollingService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 기본 10분 — Arduino 전송 주기와 비슷한 수준으로 우선 잡음, SmartThings API rate limit
    // 상황을 보며 조정 필요(#133 이슈에 명시된 미확정 사항).
    @Scheduled(fixedDelayString = "\${smartthings.metric-poll-interval-ms:600000}")
    fun poll() {
        val devices = deviceRepository.findAllByDeviceType(DeviceType.SMART_THINGS)
        devices.forEach { device ->
            runCatching { smartThingsMetricPollingService.pollAndSave(device) }
                .onFailure { e -> log.warn("SmartThings 온습도 폴링 실패: deviceId={}, error={}", device.deviceKey, e.message) }
        }
    }
}
