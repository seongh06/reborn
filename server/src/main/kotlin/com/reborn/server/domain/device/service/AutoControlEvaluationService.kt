package com.reborn.server.domain.device.service

import com.reborn.server.domain.device.AutoControlRule
import com.reborn.server.domain.device.OperationMode
import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.repository.AutoControlRuleRepository
import com.reborn.server.domain.metric.MetricLogRepository
import com.reborn.server.domain.smartthings.service.SmartThingsDeviceService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

// 자동 제어 규칙 1건을 실제로 평가하고, 조건이 맞으면 SmartThings로 제어 명령을 보낸다(#190).
// 온도 상/하한 조건만 실행 대상 - 습도/재실인원/불쾌지수/자동 꺼짐은 저장만 되고 미실행(범위 밖).
@Service
class AutoControlEvaluationService(
    private val metricLogRepository: MetricLogRepository,
    private val autoControlRuleRepository: AutoControlRuleRepository,
    private val smartThingsDeviceService: SmartThingsDeviceService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        // 동일 조건을 매 tick마다 반복 실행하지 않도록 하는 쿨다운 - 스케줄러 주기(기본 10분)보다
        // 넉넉하게 잡아서 온도가 경계값 근처에서 오르내려도 과도하게 반복 트리거되지 않게 한다.
        private val TRIGGER_COOLDOWN: Duration = Duration.ofMinutes(30)
    }

    @Transactional
    fun evaluateAndApply(rule: AutoControlRule) {
        val device = rule.device
        val now = LocalDateTime.now()
        if (rule.lastTriggeredAt != null && Duration.between(rule.lastTriggeredAt, now) < TRIGGER_COOLDOWN) {
            return
        }

        val temperature = metricLogRepository.findTopByDeviceIdOrderByCreatedAtDesc(device.id)?.temperature ?: return
        val highThreshold = rule.temperatureHighThreshold?.let(::parseLeadingNumber)
        val lowThreshold = rule.temperatureLowThreshold?.let(::parseLeadingNumber)

        val request = when {
            highThreshold != null && temperature >= highThreshold -> actionToRequest(rule.temperatureHighAction)
            lowThreshold != null && temperature <= lowThreshold -> actionToRequest(rule.temperatureLowAction)
            else -> null
        } ?: return

        smartThingsDeviceService.controlInternal(device, request)
        rule.lastTriggeredAt = now
        autoControlRuleRepository.save(rule)
        log.info("자동 제어 실행: deviceId={}, temperature={}, action={}", device.deviceKey, temperature, request)
    }

    private fun parseLeadingNumber(text: String): Double? =
        Regex("-?\\d+(\\.\\d+)?").find(text)?.value?.toDoubleOrNull()

    // 클라이언트 프리셋 문구(AutoControlField.TemperatureHigh/Low의 실제 옵션)를 SmartThings 제어
    // 요청으로 매핑. 클라이언트 쪽 옵션 목록이 바뀌면 반드시 같이 갱신할 것.
    private fun actionToRequest(action: String?): DeviceDto.ControlRequest? = when (action) {
        "냉방 시작" -> DeviceDto.ControlRequest(isPowerOn = true, operationMode = OperationMode.COOL)
        "난방 시작" -> DeviceDto.ControlRequest(isPowerOn = true, operationMode = OperationMode.HEAT)
        "제습 시작" -> DeviceDto.ControlRequest(isPowerOn = true, operationMode = OperationMode.DEHUMIDIFY)
        "송풍 시작" -> DeviceDto.ControlRequest(isPowerOn = true, operationMode = OperationMode.FAN)
        "전원 끄기" -> DeviceDto.ControlRequest(isPowerOn = false)
        // "가습 시작"은 SmartThings 에어컨 커맨드 매핑 범위(#132)에 가습 capability가 없어 미실행
        else -> null
    }
}
