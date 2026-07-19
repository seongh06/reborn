package com.reborn.server.domain.smartthings.service

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.metric.MetricLog
import com.reborn.server.domain.metric.MetricLogRepository
import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.PlaceType
import com.reborn.server.domain.smartthings.client.SmartThingsDeviceClient
import com.reborn.server.domain.smartthings.client.SmartThingsDeviceStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class SmartThingsMetricPollingServiceTest {

    @Mock
    private lateinit var smartThingsService: SmartThingsService

    @Mock
    private lateinit var smartThingsDeviceClient: SmartThingsDeviceClient

    @Mock
    private lateinit var metricLogRepository: MetricLogRepository

    @InjectMocks
    private lateinit var smartThingsMetricPollingService: SmartThingsMetricPollingService

    private lateinit var place: Place
    private lateinit var device: Device

    @BeforeEach
    fun setUp() {
        place = Place(name = "테스트 거실", qrCode = "qr-test", type = PlaceType.HOME, id = 501)
        device = Device(place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "st-device-1", name = "거실 에어컨")
    }

    @Test
    fun `pollAndSave - 온습도를 지원하면 metric_logs에 저장한다`() {
        given(smartThingsService.getValidAccessToken(501L)).willReturn("access-token")
        given(smartThingsDeviceClient.getDeviceStatus("access-token", "st-device-1"))
            .willReturn(SmartThingsDeviceStatus(temperature = 24.5, humidity = 45.0))

        smartThingsMetricPollingService.pollAndSave(device)

        verify(metricLogRepository).save(Mockito.any(MetricLog::class.java))
        assertThat(device.isOnline).isTrue()
    }

    @Test
    fun `pollAndSave - 온습도 capability를 지원하지 않으면 저장하지 않고 스킵한다`() {
        given(smartThingsService.getValidAccessToken(501L)).willReturn("access-token")
        given(smartThingsDeviceClient.getDeviceStatus("access-token", "st-device-1"))
            .willReturn(SmartThingsDeviceStatus(temperature = null, humidity = null))

        smartThingsMetricPollingService.pollAndSave(device)

        verify(metricLogRepository, never()).save(Mockito.any(MetricLog::class.java))
    }

    @Test
    fun `pollAndSave - 온도만 지원해도 저장한다`() {
        given(smartThingsService.getValidAccessToken(501L)).willReturn("access-token")
        given(smartThingsDeviceClient.getDeviceStatus("access-token", "st-device-1"))
            .willReturn(SmartThingsDeviceStatus(temperature = 22.0, humidity = null))

        smartThingsMetricPollingService.pollAndSave(device)

        verify(metricLogRepository).save(Mockito.any(MetricLog::class.java))
    }
}
