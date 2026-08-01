package com.reborn.server.domain.device.service

import com.reborn.server.domain.analytics.AutoControlExecutionLog
import com.reborn.server.domain.analytics.repository.AutoControlExecutionLogRepository
import com.reborn.server.domain.device.AutoControlRule
import com.reborn.server.domain.device.DeviceType
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

// 자동 제어 규칙 1건을 실제로 평가하고, 조건이 맞으면 제어 명령을 보낸다(#190, IR은 #288로 확장).
// 온도 상/하한 조건만 실행 대상 - 습도/재실인원/불쾌지수/자동 꺼짐은 저장만 되고 미실행(범위 밖).
//
// ⚠️ IR 기기(hasIrControl=true)는 상태지정형(ON/OFF 별도 코드) 리모컨이라는 가정 하에 SmartThings와
// 동일하게 취급한다(#288) - 임계값이 계속 참이면 쿨다운마다 그냥 재전송된다. 상태지정형이면 멱등이라
// 안전하지만, 실제로는 토글형일 수도 있어(설치 후 raw capture로 확인 예정) 확인되기 전까지는 이 반복
// 재전송이 위험할 수 있다는 걸 감안할 것 - 토글형으로 밝혀지면 온도 추이 기반 효과 확인 없이 이 경로로
// 반복 호출하면 안 되고 별도 안전장치가 필요하다.
@Service
class AutoControlEvaluationService(
    private val metricLogRepository: MetricLogRepository,
    private val autoControlRuleRepository: AutoControlRuleRepository,
    private val smartThingsDeviceService: SmartThingsDeviceService,
    private val arduinoIrControlService: ArduinoIrControlService,
    private val autoControlExecutionLogRepository: AutoControlExecutionLogRepository,
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

        val action = when {
            highThreshold != null && temperature >= highThreshold -> rule.temperatureHighAction
            lowThreshold != null && temperature <= lowThreshold -> rule.temperatureLowAction
            else -> null
        } ?: return
        val request = actionToRequest(action) ?: return

        when {
            device.deviceType == DeviceType.SMART_THINGS -> smartThingsDeviceService.controlInternal(device, request)
            device.deviceType == DeviceType.ARDUINO && device.hasIrControl ->
                arduinoIrControlService.controlInternal(device, request)
            else -> return
        }
        rule.lastTriggeredAt = now
        autoControlRuleRepository.save(rule)
        autoControlExecutionLogRepository.save(
            AutoControlExecutionLog(device = device, triggeredTemperature = temperature, action = action),
        )
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
        // 에어컨 외 기기(조명/플러그/TV/커튼/기타)용 축소 자동 제어 액션 - switch capability만 사용
        "켜기" -> DeviceDto.ControlRequest(isPowerOn = true)
        "끄기" -> DeviceDto.ControlRequest(isPowerOn = false)
        else -> null
    }
}
