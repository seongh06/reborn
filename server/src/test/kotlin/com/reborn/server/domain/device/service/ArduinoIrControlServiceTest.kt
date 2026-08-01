package com.reborn.server.domain.device.service

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.IrCommand
import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.place.AccessLevel
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
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ArduinoIrControlServiceTest {

    @Mock
    private lateinit var deviceRepository: DeviceRepository

    @Mock
    private lateinit var userPlaceMappingRepository: UserPlaceMappingRepository

    @InjectMocks
    private lateinit var arduinoIrControlService: ArduinoIrControlService

    private lateinit var place: Place
    private lateinit var device: Device

    @BeforeEach
    fun setUp() {
        place = Place(name = "테스트 거실", qrCode = "qr-test", type = PlaceType.HOME, id = 501)
        device = Device(place = place, deviceType = DeviceType.ARDUINO, deviceKey = "AR001", hasIrControl = true, id = 10)
    }

    @Test
    fun `control - IR 지원 기기에 전원 끄기를 보내면 POWER_OFF가 큐잉된다`() {
        given(deviceRepository.findByDeviceKey("AR001")).willReturn(device)
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.ADMIN)

        val response = arduinoIrControlService.control(1L, "AR001", DeviceDto.ControlRequest(isPowerOn = false))

        assertThat(response.deviceId).isEqualTo("AR001")
        assertThat(device.pendingIrCommand).isEqualTo(IrCommand.POWER_OFF)
    }

    @Test
    fun `control - 온도가 지정되면 COOL_24로 큐잉된다`() {
        given(deviceRepository.findByDeviceKey("AR001")).willReturn(device)
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.ADMIN)

        arduinoIrControlService.control(1L, "AR001", DeviceDto.ControlRequest(isPowerOn = true, temperature = 24))

        assertThat(device.pendingIrCommand).isEqualTo(IrCommand.COOL_24)
    }

    @Test
    fun `control - hasIrControl이 false면 예외가 발생한다`() {
        val notIrDevice = Device(place = place, deviceType = DeviceType.ARDUINO, deviceKey = "AR002", hasIrControl = false, id = 11)
        given(deviceRepository.findByDeviceKey("AR002")).willReturn(notIrDevice)

        assertThatThrownBy { arduinoIrControlService.control(1L, "AR002", DeviceDto.ControlRequest(isPowerOn = true)) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `control - ADMIN 권한이 없으면 예외가 발생한다`() {
        given(deviceRepository.findByDeviceKey("AR001")).willReturn(device)
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.USER)

        assertThatThrownBy { arduinoIrControlService.control(1L, "AR001", DeviceDto.ControlRequest(isPowerOn = true)) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.FORBIDDEN)
    }

    @Test
    fun `consumePendingCommand - 대기 중인 명령을 반환하고 비운다`() {
        device.queuePendingIrCommand(IrCommand.POWER_ON)
        given(deviceRepository.findByDeviceKey("AR001")).willReturn(device)

        val response = arduinoIrControlService.consumePendingCommand("AR001")

        assertThat(response.command).isEqualTo("POWER_ON")
        assertThat(device.pendingIrCommand).isNull()
    }

    @Test
    fun `consumePendingCommand - 대기 중인 명령이 없으면 null을 반환한다`() {
        given(deviceRepository.findByDeviceKey("AR001")).willReturn(device)

        val response = arduinoIrControlService.consumePendingCommand("AR001")

        assertThat(response.command).isNull()
    }

    @Test
    fun `consumePendingCommand - 존재하지 않는 기기면 예외가 발생한다`() {
        given(deviceRepository.findByDeviceKey("UNKNOWN")).willReturn(null)

        assertThatThrownBy { arduinoIrControlService.consumePendingCommand("UNKNOWN") }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }
}
