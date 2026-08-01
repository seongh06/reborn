package com.reborn.server.domain.device.service

import com.reborn.server.domain.analytics.AutoControlExecutionLog
import com.reborn.server.domain.analytics.repository.AutoControlExecutionLogRepository
import com.reborn.server.domain.device.AutoControlRule
import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.OperationMode
import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.repository.AutoControlRuleRepository
import com.reborn.server.domain.metric.MetricLog
import com.reborn.server.domain.metric.MetricLogRepository
import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.PlaceType
import com.reborn.server.domain.smartthings.service.SmartThingsDeviceService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

// Mockito의 any()는 Device/ControlRequest/AutoControlRule 같은 Kotlin non-null 참조 타입
// 인자에서 null을 반환해 "must not be null" NPE를 유발한다(DeviceServiceTest의 anyDuration()과 동일 이슈).
private fun anyControlRequest(): DeviceDto.ControlRequest {
    Mockito.any(DeviceDto.ControlRequest::class.java)
    return DeviceDto.ControlRequest()
}

// anyDevice()를 재사용하면 내부 Mockito.any() 호출이 하나 더 스택에 쌓여 "N matchers expected,
// N+1 recorded" 오류가 나므로, 각 헬퍼는 자신의 매처 등록만 하고 값은 직접 생성한다.
private fun dummyDevice(): Device = Device(
    place = Place(name = "", qrCode = "", type = PlaceType.HOME, id = 0),
    deviceType = DeviceType.SMART_THINGS,
    deviceKey = "",
)

private fun anyAutoControlRule(): AutoControlRule {
    Mockito.any(AutoControlRule::class.java)
    return AutoControlRule(device = dummyDevice())
}

private fun anyDevice(): Device {
    Mockito.any(Device::class.java)
    return dummyDevice()
}

// ArgumentCaptor.capture()도 동일한 이유로 NPE를 유발한다(SmartThingsDeviceServiceTest와 동일 이슈) -
// 매처는 등록하되 non-null 더미를 반환.
private fun captureExecutionLog(captor: org.mockito.ArgumentCaptor<AutoControlExecutionLog>): AutoControlExecutionLog {
    captor.capture()
    return AutoControlExecutionLog(device = dummyDevice(), triggeredTemperature = 0.0, action = "")
}

private fun anyExecutionLog(): AutoControlExecutionLog {
    Mockito.any(AutoControlExecutionLog::class.java)
    return AutoControlExecutionLog(device = dummyDevice(), triggeredTemperature = 0.0, action = "")
}

@ExtendWith(MockitoExtension::class)
class AutoControlEvaluationServiceTest {

    @Mock
    private lateinit var metricLogRepository: MetricLogRepository

    @Mock
    private lateinit var autoControlRuleRepository: AutoControlRuleRepository

    @Mock
    private lateinit var smartThingsDeviceService: SmartThingsDeviceService

    @Mock
    private lateinit var arduinoIrControlService: ArduinoIrControlService

    @Mock
    private lateinit var autoControlExecutionLogRepository: AutoControlExecutionLogRepository

    private lateinit var evaluationService: AutoControlEvaluationService

    private lateinit var place: Place
    private lateinit var device: Device

    @BeforeEach
    fun setUp() {
        place = Place(name = "테스트 거실", qrCode = "qr-test", type = PlaceType.HOME, id = 501)
        device = Device(place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "ST001", id = 10)
        evaluationService = AutoControlEvaluationService(
            metricLogRepository = metricLogRepository,
            autoControlRuleRepository = autoControlRuleRepository,
            smartThingsDeviceService = smartThingsDeviceService,
            arduinoIrControlService = arduinoIrControlService,
            autoControlExecutionLogRepository = autoControlExecutionLogRepository,
        )
    }

    @Test
    fun `evaluateAndApply - 온도가 상한을 넘으면 상한 액션으로 제어한다`() {
        val rule = AutoControlRule(
            device = device,
            temperatureHighThreshold = "28도",
            temperatureHighAction = "냉방 시작",
        )
        given(metricLogRepository.findTopByDeviceIdOrderByCreatedAtDesc(10L))
            .willReturn(MetricLog(device = device, temperature = 29.5))

        evaluationService.evaluateAndApply(rule)

        val expected = DeviceDto.ControlRequest(isPowerOn = true, operationMode = OperationMode.COOL)
        verify(smartThingsDeviceService).controlInternal(device, expected)
        assertThat(rule.lastTriggeredAt).isNotNull()
        verify(autoControlRuleRepository).save(rule)

        val logCaptor = org.mockito.ArgumentCaptor.forClass(AutoControlExecutionLog::class.java)
        verify(autoControlExecutionLogRepository).save(captureExecutionLog(logCaptor))
        assertThat(logCaptor.value.device).isEqualTo(device)
        assertThat(logCaptor.value.triggeredTemperature).isEqualTo(29.5)
        assertThat(logCaptor.value.action).isEqualTo("냉방 시작")
    }

    @Test
    fun `evaluateAndApply - 온도가 하한 밑이면 하한 액션으로 제어한다`() {
        val rule = AutoControlRule(
            device = device,
            temperatureLowThreshold = "18",
            temperatureLowAction = "난방 시작",
        )
        given(metricLogRepository.findTopByDeviceIdOrderByCreatedAtDesc(10L))
            .willReturn(MetricLog(device = device, temperature = 15.0))

        evaluationService.evaluateAndApply(rule)

        val expected = DeviceDto.ControlRequest(isPowerOn = true, operationMode = OperationMode.HEAT)
        verify(smartThingsDeviceService).controlInternal(device, expected)
    }

    @Test
    fun `evaluateAndApply - 비-에어컨용 켜기 끄기 액션도 switch 커맨드로 제어한다`() {
        val highRule = AutoControlRule(
            device = device,
            temperatureHighThreshold = "28",
            temperatureHighAction = "끄기",
        )
        given(metricLogRepository.findTopByDeviceIdOrderByCreatedAtDesc(10L))
            .willReturn(MetricLog(device = device, temperature = 29.0))

        evaluationService.evaluateAndApply(highRule)

        verify(smartThingsDeviceService).controlInternal(device, DeviceDto.ControlRequest(isPowerOn = false))
    }

    @Test
    fun `evaluateAndApply - 임계값을 넘지 않으면 제어하지 않는다`() {
        val rule = AutoControlRule(device = device, temperatureHighThreshold = "28", temperatureHighAction = "냉방 시작")
        given(metricLogRepository.findTopByDeviceIdOrderByCreatedAtDesc(10L))
            .willReturn(MetricLog(device = device, temperature = 24.0))

        evaluationService.evaluateAndApply(rule)

        verify(smartThingsDeviceService, never()).controlInternal(anyDevice(), anyControlRequest())
        verify(autoControlRuleRepository, never()).save(anyAutoControlRule())
        verify(autoControlExecutionLogRepository, never()).save(anyExecutionLog())
    }

    @Test
    fun `evaluateAndApply - 최근 메트릭이 없으면 아무 것도 하지 않는다`() {
        val rule = AutoControlRule(device = device, temperatureHighThreshold = "28", temperatureHighAction = "냉방 시작")
        given(metricLogRepository.findTopByDeviceIdOrderByCreatedAtDesc(10L)).willReturn(null)

        evaluationService.evaluateAndApply(rule)

        verify(smartThingsDeviceService, never()).controlInternal(anyDevice(), anyControlRequest())
    }

    @Test
    fun `evaluateAndApply - 쿨다운 이내에 다시 트리거되지 않는다`() {
        val rule = AutoControlRule(
            device = device,
            temperatureHighThreshold = "28",
            temperatureHighAction = "냉방 시작",
            lastTriggeredAt = LocalDateTime.now().minusMinutes(5),
        )

        evaluationService.evaluateAndApply(rule)

        verify(metricLogRepository, never()).findTopByDeviceIdOrderByCreatedAtDesc(anyLong())
        verify(smartThingsDeviceService, never()).controlInternal(anyDevice(), anyControlRequest())
    }
}
