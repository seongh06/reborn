package com.reborn.server.domain.metric.service

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.googlesheets.client.GoogleSheetsWriterClient
import com.reborn.server.domain.googlesheets.service.GoogleSheetsService
import com.reborn.server.domain.metric.MetricLog
import com.reborn.server.domain.metric.MetricLogRepository
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
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

// DeviceServiceTest의 anyDuration()과 동일 이슈 - Pageable도 Kotlin non-null 참조 타입 인자라
// Mockito any()가 null을 반환해 NPE를 유발한다.
private fun anyPageable(): Pageable {
    Mockito.any(Pageable::class.java)
    return PageRequest.of(0, 1)
}

private fun anyValues(): List<List<Any?>> {
    Mockito.anyList<List<Any?>>()
    return emptyList()
}

// SmartThingsServiceTest의 eqString()과 동일 이슈 - Mockito eq()도 String 같은 Kotlin
// non-null 참조 타입 인자에서 null을 반환해 NPE를 유발한다.
private fun eqString(value: String): String {
    Mockito.eq(value)
    return value
}

@ExtendWith(MockitoExtension::class)
class MetricExportServiceTest {

    @Mock
    private lateinit var deviceRepository: DeviceRepository

    @Mock
    private lateinit var metricLogRepository: MetricLogRepository

    @Mock
    private lateinit var userPlaceMappingRepository: UserPlaceMappingRepository

    @Mock
    private lateinit var googleSheetsService: GoogleSheetsService

    @Mock
    private lateinit var googleSheetsWriterClient: GoogleSheetsWriterClient

    @InjectMocks
    private lateinit var metricExportService: MetricExportService

    private lateinit var place: Place
    private lateinit var device: Device

    @BeforeEach
    fun setUp() {
        place = Place(name = "테스트 거실", qrCode = "qr-test", type = PlaceType.HOME, id = 501)
        device = Device(place = place, deviceType = DeviceType.ARDUINO, deviceKey = "arduino_room_01", name = "거실", id = 10)
    }

    @Test
    fun `exportToSheets - 연동된 장소면 스프레드시트를 만들어 URL을 반환한다`() {
        val log = MetricLog(device = device, temperature = 26.5, humidity = 62.3, id = 1).apply { prePersist() }
        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(userPlaceMappingRepository.existsByUserIdAndPlaceId(1L, 501L)).willReturn(true)
        given(googleSheetsService.getValidAccessToken(501L)).willReturn("access-token")
        given(metricLogRepository.findAllByDeviceId(eq(10L), anyPageable()))
            .willReturn(PageImpl(listOf(log)))
        given(googleSheetsWriterClient.createSpreadsheet(anyString(), anyString())).willReturn("sheet-id-123")

        val response = metricExportService.exportToSheets("arduino_room_01", 1L)

        assertThat(response.spreadsheetUrl).isEqualTo("https://docs.google.com/spreadsheets/d/sheet-id-123/edit")
        verify(googleSheetsWriterClient).writeValues(eqString("access-token"), eqString("sheet-id-123"), eqString("A1"), anyValues())
    }

    @Test
    fun `exportToSheets - 등록되지 않은 기기면 예외가 발생한다`() {
        given(deviceRepository.findByDeviceKey("unknown")).willReturn(null)

        assertThatThrownBy { metricExportService.exportToSheets("unknown", 1L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `exportToSheets - 장소 접근 권한이 없으면 예외가 발생한다`() {
        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(userPlaceMappingRepository.existsByUserIdAndPlaceId(1L, 501L)).willReturn(false)

        assertThatThrownBy { metricExportService.exportToSheets("arduino_room_01", 1L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.FORBIDDEN)
    }

    @Test
    fun `exportToSheets - Google Sheets 미연동이면 예외가 발생한다`() {
        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(userPlaceMappingRepository.existsByUserIdAndPlaceId(1L, 501L)).willReturn(true)
        given(googleSheetsService.getValidAccessToken(501L))
            .willThrow(BusinessAlertException(CommonErrorCode.NOT_FOUND, "이 장소는 Google Sheets와 연동되어 있지 않습니다."))

        assertThatThrownBy { metricExportService.exportToSheets("arduino_room_01", 1L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }
}
