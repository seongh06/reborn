package com.reborn.server.domain.metric.service

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.metric.MetricLog
import com.reborn.server.domain.metric.MetricLogRepository
import com.reborn.server.domain.metric.dto.MetricDto
import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.PlaceType
import com.reborn.server.domain.place.UserPlaceMappingRepository
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class MetricServiceTest {

    @Mock
    private lateinit var deviceRepository: DeviceRepository

    @Mock
    private lateinit var metricLogRepository: MetricLogRepository

    @Mock
    private lateinit var userPlaceMappingRepository: UserPlaceMappingRepository

    @InjectMocks
    private lateinit var metricService: MetricService

    private lateinit var device: Device

    @BeforeEach
    fun setUp() {
        val place = Place(name = "테스트 거실", qrCode = "qr-test", type = PlaceType.HOME)
        device = Device(place = place, deviceType = DeviceType.ARDUINO, deviceKey = "arduino_room_01", name = "거실")
    }

    @Test
    fun `collect - 등록된 기기면 메트릭을 저장하고 불쾌지수를 계산해 반환한다`() {
        val request = MetricDto.CollectRequest(
            temperature = 26.5,
            humidity = 62.3,
            illuminance = 480,
            peopleCount = 3,
        )
        val savedLog = MetricLog(
            device = device,
            temperature = request.temperature,
            humidity = request.humidity,
            illuminance = request.illuminance,
            occupancy = request.peopleCount,
            id = 10023,
        ).apply { prePersist() }

        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(metricLogRepository.save(any())).willReturn(savedLog)

        val response = metricService.collect("arduino_room_01", request)

        assertThat(response.logId).isEqualTo(10023L)
        assertThat(response.discomfort).isEqualTo(75.2)
        assertThat(response.createdAt).isEqualTo(savedLog.createdAt)
        assertThat(device.isOnline).isTrue()
    }

    @Test
    fun `collect - 등록되지 않은 기기면 예외가 발생한다`() {
        val request = MetricDto.CollectRequest(temperature = 26.5, humidity = 62.3)
        given(deviceRepository.findByDeviceKey("unknown_device")).willReturn(null)

        assertThatThrownBy { metricService.collect("unknown_device", request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `collect - 측정값이 모두 비어있으면 예외가 발생한다`() {
        val request = MetricDto.CollectRequest()

        assertThatThrownBy { metricService.collect("arduino_room_01", request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `collect - 습도가 0~100 범위를 벗어나면 예외가 발생한다`() {
        val request = MetricDto.CollectRequest(humidity = 120.0)

        assertThatThrownBy { metricService.collect("arduino_room_01", request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `collect - 조도 또는 재실 인원이 음수면 예외가 발생한다`() {
        val request = MetricDto.CollectRequest(illuminance = -10)

        assertThatThrownBy { metricService.collect("arduino_room_01", request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `getCurrent - 등록된 기기의 최신 메트릭을 반환한다`() {
        val latestLog = MetricLog(
            device = device,
            temperature = 26.5,
            humidity = 62.3,
            illuminance = 480,
            occupancy = 3,
            id = 10023,
        ).apply { prePersist() }

        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(metricLogRepository.findTopByDeviceIdOrderByCreatedAtDesc(device.id)).willReturn(latestLog)

        val response = metricService.getCurrent("arduino_room_01")

        assertThat(response.deviceId).isEqualTo("arduino_room_01")
        assertThat(response.deviceName).isEqualTo("거실")
        assertThat(response.peopleCount).isEqualTo(3)
        assertThat(response.discomfort).isEqualTo(75.2)
    }

    @Test
    fun `getCurrent - 등록되지 않은 기기면 예외가 발생한다`() {
        given(deviceRepository.findByDeviceKey("unknown_device")).willReturn(null)

        assertThatThrownBy { metricService.getCurrent("unknown_device") }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `getCurrent - 수집된 데이터가 없으면 예외가 발생한다`() {
        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(metricLogRepository.findTopByDeviceIdOrderByCreatedAtDesc(device.id)).willReturn(null)

        assertThatThrownBy { metricService.getCurrent("arduino_room_01") }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `getHistory - 접근 권한이 있으면 페이징된 로그 목록을 반환한다`() {
        val pageable = PageRequest.of(0, 20)
        val log = MetricLog(
            device = device,
            temperature = 26.5,
            humidity = 62.3,
            illuminance = 480,
            occupancy = 3,
            id = 10023,
        ).apply { prePersist() }

        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(userPlaceMappingRepository.existsByUserIdAndPlaceId(1L, device.place.id)).willReturn(true)
        given(metricLogRepository.findAllByDeviceId(device.id, pageable)).willReturn(PageImpl(listOf(log), pageable, 1))

        val response = metricService.getHistory("arduino_room_01", 1L, pageable)

        assertThat(response.deviceId).isEqualTo("arduino_room_01")
        assertThat(response.logs).hasSize(1)
        assertThat(response.logs[0].logId).isEqualTo(10023L)
        assertThat(response.totalElements).isEqualTo(1L)
    }

    @Test
    fun `getHistory - 등록되지 않은 기기면 예외가 발생한다`() {
        val pageable = PageRequest.of(0, 20)
        given(deviceRepository.findByDeviceKey("unknown_device")).willReturn(null)

        assertThatThrownBy { metricService.getHistory("unknown_device", 1L, pageable) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `getHistory - 장소 접근 권한이 없으면 예외가 발생한다`() {
        val pageable = PageRequest.of(0, 20)
        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(userPlaceMappingRepository.existsByUserIdAndPlaceId(1L, device.place.id)).willReturn(false)

        assertThatThrownBy { metricService.getHistory("arduino_room_01", 1L, pageable) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.FORBIDDEN)
    }
}
