package com.reborn.server.domain.smartthings.service

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.PlaceType
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class SmartThingsMetricPollingSchedulerTest {

    @Mock
    private lateinit var deviceRepository: DeviceRepository

    @Mock
    private lateinit var smartThingsMetricPollingService: SmartThingsMetricPollingService

    @InjectMocks
    private lateinit var scheduler: SmartThingsMetricPollingScheduler

    private val place = Place(name = "테스트 거실", qrCode = "qr-test", type = PlaceType.HOME, id = 501)

    @Test
    fun `poll - SMART_THINGS 기기 전부를 폴링한다`() {
        val device1 = Device(place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "st-1", name = "에어컨")
        val device2 = Device(place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "st-2", name = "공기청정기")
        given(deviceRepository.findAllByDeviceType(DeviceType.SMART_THINGS)).willReturn(listOf(device1, device2))

        scheduler.poll()

        verify(smartThingsMetricPollingService).pollAndSave(device1)
        verify(smartThingsMetricPollingService).pollAndSave(device2)
    }

    @Test
    fun `poll - 한 기기가 실패해도 나머지는 계속 폴링한다`() {
        val device1 = Device(place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "st-1", name = "에어컨")
        val device2 = Device(place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "st-2", name = "공기청정기")
        given(deviceRepository.findAllByDeviceType(DeviceType.SMART_THINGS)).willReturn(listOf(device1, device2))
        doThrow(RuntimeException("SmartThings 오류")).`when`(smartThingsMetricPollingService).pollAndSave(device1)

        assertThatCode { scheduler.poll() }.doesNotThrowAnyException()

        verify(smartThingsMetricPollingService, times(1)).pollAndSave(device1)
        verify(smartThingsMetricPollingService, times(1)).pollAndSave(device2)
    }
}
