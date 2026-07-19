package com.reborn.server.domain.smartthings.service

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.metric.MetricLog
import com.reborn.server.domain.metric.MetricLogRepository
import com.reborn.server.domain.smartthings.client.SmartThingsDeviceClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// Arduino/공기계가 없는 장소를 위한 SmartThings 온습도 폴링(#133). Arduino의 push(POST
// /api/metric/collect)와 다르게 서버가 주기적으로 상태를 끌어와(pull) 동일한 metric_logs에 적재한다.
//
// 한 장소에 Arduino와 SMART_THINGS 온습도 소스가 동시에 있는 경우의 우선순위/중복 처리 정책은
// 아직 미정(#133 이슈 참고) — 지금은 SMART_THINGS 타입 기기를 무조건 전부 폴링한다.
//
// pollAndSave는 기기별로 SmartThingsMetricPollingScheduler(다른 빈)에서 호출된다 — 같은
// 클래스 안에서 self-invocation으로 호출하면 @Transactional 프록시가 적용되지 않으므로 주의.
@Service
class SmartThingsMetricPollingService(
    private val smartThingsService: SmartThingsService,
    private val smartThingsDeviceClient: SmartThingsDeviceClient,
    private val metricLogRepository: MetricLogRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun pollAndSave(device: Device) {
        val accessToken = smartThingsService.getValidAccessToken(device.place.id)
        val status = smartThingsDeviceClient.getDeviceStatus(accessToken, device.deviceKey)

        if (status.temperature == null && status.humidity == null) {
            log.debug("SmartThings 기기가 온습도 capability를 지원하지 않아 스킵: deviceId={}", device.deviceKey)
            return
        }

        metricLogRepository.save(
            MetricLog(
                device = device,
                temperature = status.temperature,
                humidity = status.humidity,
            ),
        )
        device.updateOnlineStatus(true)
    }
}
