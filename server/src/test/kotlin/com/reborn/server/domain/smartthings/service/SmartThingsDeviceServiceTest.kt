package com.reborn.server.domain.smartthings.service

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.OperationMode
import com.reborn.server.domain.device.WindSpeed
import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.PlaceType
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.domain.smartthings.client.SmartThingsCommand
import com.reborn.server.domain.smartthings.client.SmartThingsDeviceClient
import com.reborn.server.domain.smartthings.client.SmartThingsDeviceStatus
import com.reborn.server.domain.smartthings.client.SmartThingsDeviceSummary
import com.reborn.server.domain.smartthings.dto.SmartThingsDto
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.dao.DataIntegrityViolationException
import java.util.Optional

// Mockito의 eq()는 Kotlin non-null 참조 타입 인자에서 null을 반환해 NPE를 유발한다
// (SmartThingsServiceTest와 동일 이슈). 커스텀 매처 헬퍼로 우회.
private fun eqStr(value: String): String {
    Mockito.eq(value)
    return value
}

// ArgumentCaptor.capture()도 동일한 이유로 NPE를 유발한다 — 매처는 등록하되 non-null 더미를 반환.
private fun captureCommands(captor: org.mockito.ArgumentCaptor<List<SmartThingsCommand>>): List<SmartThingsCommand> {
    captor.capture()
    return emptyList()
}

@ExtendWith(MockitoExtension::class)
class SmartThingsDeviceServiceTest {

    @Mock
    private lateinit var smartThingsService: SmartThingsService

    @Mock
    private lateinit var smartThingsDeviceClient: SmartThingsDeviceClient

    @Mock
    private lateinit var placeRepository: PlaceRepository

    @Mock
    private lateinit var userPlaceMappingRepository: UserPlaceMappingRepository

    @Mock
    private lateinit var deviceRepository: DeviceRepository

    @InjectMocks
    private lateinit var smartThingsDeviceService: SmartThingsDeviceService

    private lateinit var place: Place

    @BeforeEach
    fun setUp() {
        place = Place(name = "테스트 거실", qrCode = "qr-test", type = PlaceType.HOME, id = 501)
    }

    @Test
    fun `listDevices - ADMIN이면 SmartThings 기기 목록을 반환한다`() {
        given(placeRepository.existsById(501L)).willReturn(true)
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.ADMIN)
        given(smartThingsService.getValidAccessToken(501L)).willReturn("access-token")
        given(smartThingsDeviceClient.getDevices("access-token"))
            .willReturn(listOf(SmartThingsDeviceSummary(deviceId = "st-device-1", label = "거실 에어컨")))

        val response = smartThingsDeviceService.listDevices(1L, 501L)

        assertThat(response.devices).hasSize(1)
        assertThat(response.devices[0].deviceId).isEqualTo("st-device-1")
        assertThat(response.devices[0].label).isEqualTo("거실 에어컨")
    }

    @Test
    fun `listDevices - ADMIN 권한이 없으면 예외가 발생한다`() {
        given(placeRepository.existsById(501L)).willReturn(true)
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.USER)

        assertThatThrownBy { smartThingsDeviceService.listDevices(1L, 501L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.FORBIDDEN)
    }

    @Test
    fun `registerDevice - 유효하면 SMART_THINGS 기기를 등록한다`() {
        val request = SmartThingsDto.RegisterDeviceRequest(placeId = 501, smartThingsDeviceId = "st-device-1", deviceName = "거실 에어컨")
        val savedDevice = Device(
            place = place,
            deviceType = DeviceType.SMART_THINGS,
            deviceKey = "st-device-1",
            name = "거실 에어컨",
            isOnline = true,
        ).apply { prePersist() }

        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.ADMIN)
        given(deviceRepository.save(Mockito.any(Device::class.java))).willReturn(savedDevice)

        val response = smartThingsDeviceService.registerDevice(1L, request)

        assertThat(response.deviceId).isEqualTo("st-device-1")
        assertThat(response.deviceType).isEqualTo("SMART_THINGS")
    }

    @Test
    fun `registerDevice - category를 지정하면 기기에 저장한다`() {
        val request = SmartThingsDto.RegisterDeviceRequest(
            placeId = 501,
            smartThingsDeviceId = "st-device-1",
            deviceName = "거실 에어컨",
            category = "AIR_CONDITIONER",
        )
        val savedDevice = Device(
            place = place,
            deviceType = DeviceType.SMART_THINGS,
            deviceKey = "st-device-1",
            name = "거실 에어컨",
            category = "AIR_CONDITIONER",
            isOnline = true,
        ).apply { prePersist() }

        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.ADMIN)
        given(deviceRepository.save(Mockito.any(Device::class.java))).willReturn(savedDevice)

        val response = smartThingsDeviceService.registerDevice(1L, request)

        assertThat(response.category).isEqualTo("AIR_CONDITIONER")
    }

    @Test
    fun `registerDevice - 잘못된 category면 예외가 발생한다`() {
        val request = SmartThingsDto.RegisterDeviceRequest(
            placeId = 501,
            smartThingsDeviceId = "st-device-1",
            deviceName = "거실 에어컨",
            category = "HEATER",
        )

        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.ADMIN)

        assertThatThrownBy { smartThingsDeviceService.registerDevice(1L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `registerDevice - 이미 등록된 기기면 예외가 발생한다`() {
        val request = SmartThingsDto.RegisterDeviceRequest(placeId = 501, smartThingsDeviceId = "st-device-1", deviceName = "거실 에어컨")

        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.ADMIN)
        given(deviceRepository.save(Mockito.any(Device::class.java))).willThrow(DataIntegrityViolationException("duplicate"))

        assertThatThrownBy { smartThingsDeviceService.registerDevice(1L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.CONFLICT)
    }

    @Test
    fun `control - 전원과 온도를 함께 보내면 커맨드 2개를 한 번에 전송한다`() {
        val device = Device(place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "st-device-1", name = "거실 에어컨")
        val request = DeviceDto.ControlRequest(isPowerOn = true, temperature = 24)

        given(deviceRepository.findByDeviceKey("st-device-1")).willReturn(device)
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.ADMIN)
        given(smartThingsService.getValidAccessToken(501L)).willReturn("access-token")

        val response = smartThingsDeviceService.control(1L, "st-device-1", request)

        assertThat(response.deviceId).isEqualTo("st-device-1")

        @Suppress("UNCHECKED_CAST")
        val captor = org.mockito.ArgumentCaptor.forClass(List::class.java) as org.mockito.ArgumentCaptor<List<SmartThingsCommand>>
        verify(smartThingsDeviceClient).sendCommands(eqStr("access-token"), eqStr("st-device-1"), captureCommands(captor))
        val commands = captor.value
        assertThat(commands).hasSize(2)
        assertThat(commands.map { it.capability }).containsExactlyInAnyOrder("switch", "thermostatCoolingSetpoint")
    }

    @Test
    fun `control - 운전모드와 풍량을 SmartThings 값으로 매핑한다`() {
        val device = Device(place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "st-device-1", name = "거실 에어컨")
        val request = DeviceDto.ControlRequest(operationMode = OperationMode.DEHUMIDIFY, windSpeed = WindSpeed.HIGH)

        given(deviceRepository.findByDeviceKey("st-device-1")).willReturn(device)
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.ADMIN)
        given(smartThingsService.getValidAccessToken(501L)).willReturn("access-token")

        smartThingsDeviceService.control(1L, "st-device-1", request)

        @Suppress("UNCHECKED_CAST")
        val captor = org.mockito.ArgumentCaptor.forClass(List::class.java) as org.mockito.ArgumentCaptor<List<SmartThingsCommand>>
        verify(smartThingsDeviceClient).sendCommands(eqStr("access-token"), eqStr("st-device-1"), captureCommands(captor))
        val commands = captor.value

        val modeCommand = commands.first { it.capability == "airConditionerMode" }
        assertThat(modeCommand.arguments).containsExactly("dry")

        val fanCommand = commands.first { it.capability == "airConditionerFanMode" }
        assertThat(fanCommand.arguments).containsExactly("high")
    }

    @Test
    fun `control - 제어할 항목이 없으면 예외가 발생한다`() {
        val device = Device(place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "st-device-1", name = "거실 에어컨")
        given(deviceRepository.findByDeviceKey("st-device-1")).willReturn(device)
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.ADMIN)

        assertThatThrownBy { smartThingsDeviceService.control(1L, "st-device-1", DeviceDto.ControlRequest()) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `control - SmartThings 기기가 아니면 예외가 발생한다`() {
        val device = Device(place = place, deviceType = DeviceType.ARDUINO, deviceKey = "arduino-1", name = "거실 센서")
        given(deviceRepository.findByDeviceKey("arduino-1")).willReturn(device)

        assertThatThrownBy { smartThingsDeviceService.control(1L, "arduino-1", DeviceDto.ControlRequest(isPowerOn = true)) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `control - 존재하지 않는 기기면 예외가 발생한다`() {
        given(deviceRepository.findByDeviceKey("unknown")).willReturn(null)

        assertThatThrownBy { smartThingsDeviceService.control(1L, "unknown", DeviceDto.ControlRequest(isPowerOn = true)) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `control - ADMIN 권한이 없으면 예외가 발생한다`() {
        val device = Device(place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "st-device-1", name = "거실 에어컨")
        given(deviceRepository.findByDeviceKey("st-device-1")).willReturn(device)
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.USER)

        assertThatThrownBy { smartThingsDeviceService.control(1L, "st-device-1", DeviceDto.ControlRequest(isPowerOn = true)) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.FORBIDDEN)
    }

    @Test
    fun `getStatus - 전체 지원 기기면 모든 필드를 반환한다`() {
        val device = Device(place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "st-device-1", name = "거실 에어컨")
        given(deviceRepository.findByDeviceKey("st-device-1")).willReturn(device)
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.ADMIN)
        given(smartThingsService.getValidAccessToken(501L)).willReturn("access-token")
        given(smartThingsDeviceClient.getDeviceStatus("access-token", "st-device-1")).willReturn(
            SmartThingsDeviceStatus(
                temperature = 24.0,
                humidity = 50.0,
                isPowerOn = true,
                operationMode = "cool",
                windSpeed = "high",
                targetTemperature = 22.0,
            ),
        )

        val response = smartThingsDeviceService.getStatus(1L, "st-device-1")

        assertThat(response.isPowerOn).isTrue()
        assertThat(response.operationMode).isEqualTo(OperationMode.COOL)
        assertThat(response.windSpeed).isEqualTo(WindSpeed.HIGH)
        assertThat(response.temperature).isEqualTo(22.0)
    }

    @Test
    fun `getStatus - 바람세기를 지원하지 않는 기기면 windSpeed가 null이다`() {
        val device = Device(place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "st-device-1", name = "거실 플러그")
        given(deviceRepository.findByDeviceKey("st-device-1")).willReturn(device)
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.ADMIN)
        given(smartThingsService.getValidAccessToken(501L)).willReturn("access-token")
        given(smartThingsDeviceClient.getDeviceStatus("access-token", "st-device-1")).willReturn(
            SmartThingsDeviceStatus(
                temperature = null,
                humidity = null,
                isPowerOn = true,
                operationMode = null,
                windSpeed = null,
                targetTemperature = null,
            ),
        )

        val response = smartThingsDeviceService.getStatus(1L, "st-device-1")

        assertThat(response.isPowerOn).isTrue()
        assertThat(response.operationMode).isNull()
        assertThat(response.windSpeed).isNull()
        assertThat(response.temperature).isNull()
    }

    @Test
    fun `getStatus - SmartThings 기기가 아니면 예외가 발생한다`() {
        val device = Device(place = place, deviceType = DeviceType.ARDUINO, deviceKey = "arduino-1", name = "거실 센서")
        given(deviceRepository.findByDeviceKey("arduino-1")).willReturn(device)

        assertThatThrownBy { smartThingsDeviceService.getStatus(1L, "arduino-1") }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `getStatus - 존재하지 않는 기기면 예외가 발생한다`() {
        given(deviceRepository.findByDeviceKey("unknown")).willReturn(null)

        assertThatThrownBy { smartThingsDeviceService.getStatus(1L, "unknown") }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `getStatus - ADMIN 권한이 없으면 예외가 발생한다`() {
        val device = Device(place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "st-device-1", name = "거실 에어컨")
        given(deviceRepository.findByDeviceKey("st-device-1")).willReturn(device)
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.USER)

        assertThatThrownBy { smartThingsDeviceService.getStatus(1L, "st-device-1") }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.FORBIDDEN)
    }
}
