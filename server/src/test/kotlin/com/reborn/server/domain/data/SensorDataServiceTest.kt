package com.reborn.server.domain.data

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.place.Place
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class SensorDataServiceTest {

    @Mock
    private lateinit var deviceRepository: DeviceRepository

    @Mock
    private lateinit var sensorLogsRepository: SensorLogsRepository

    @InjectMocks
    private lateinit var sensorDataService: SensorDataService

    private lateinit var device: Device

    @BeforeEach
    fun setUp() {
        val place = Place(name = "테스트 거실", qrCode = "qr-test")
        device = Device(place = place, deviceType = DeviceType.ARDUINO, deviceKey = "arduino_room_01", name = "거실")
    }

    @Test
    fun `collect - 등록된 기기면 센서 데이터를 저장하고 불쾌지수를 계산해 반환한다`() {
        val request = SensorDataDto.CollectRequest(
            temperature = 26.5,
            humidity = 62.3,
            illuminance = 480,
            peopleCount = 3,
        )
        val savedLog = SensorLogs(
            device = device,
            temperature = request.temperature,
            humidity = request.humidity,
            illuminance = request.illuminance,
            occupancy = request.peopleCount,
            id = 10023,
        ).apply { prePersist() }

        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(sensorLogsRepository.save(any())).willReturn(savedLog)

        val response = sensorDataService.collect("arduino_room_01", request)

        assertThat(response.logId).isEqualTo(10023L)
        assertThat(response.discomfort).isEqualTo(75.2)
        assertThat(response.createdAt).isEqualTo(savedLog.createdAt)
        assertThat(device.isOnline).isTrue()
    }

    @Test
    fun `collect - 등록되지 않은 기기면 예외가 발생한다`() {
        val request = SensorDataDto.CollectRequest(temperature = 26.5, humidity = 62.3)
        given(deviceRepository.findByDeviceKey("unknown_device")).willReturn(null)

        assertThatThrownBy { sensorDataService.collect("unknown_device", request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `getCurrent - 등록된 기기의 최신 센서 데이터를 반환한다`() {
        val latestLog = SensorLogs(
            device = device,
            temperature = 26.5,
            humidity = 62.3,
            illuminance = 480,
            occupancy = 3,
            id = 10023,
        ).apply { prePersist() }

        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(sensorLogsRepository.findTopByDeviceIdOrderByCreatedAtDesc(device.id)).willReturn(latestLog)

        val response = sensorDataService.getCurrent("arduino_room_01")

        assertThat(response.deviceId).isEqualTo("arduino_room_01")
        assertThat(response.deviceName).isEqualTo("거실")
        assertThat(response.peopleCount).isEqualTo(3)
        assertThat(response.discomfort).isEqualTo(75.2)
    }

    @Test
    fun `getCurrent - 등록되지 않은 기기면 예외가 발생한다`() {
        given(deviceRepository.findByDeviceKey("unknown_device")).willReturn(null)

        assertThatThrownBy { sensorDataService.getCurrent("unknown_device") }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `getCurrent - 수집된 데이터가 없으면 예외가 발생한다`() {
        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(sensorLogsRepository.findTopByDeviceIdOrderByCreatedAtDesc(device.id)).willReturn(null)

        assertThatThrownBy { sensorDataService.getCurrent("arduino_room_01") }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }
}
