package com.reborn.server.domain.feedback.service

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.auth.OAuthProvider
import com.reborn.server.domain.auth.User
import com.reborn.server.domain.feedback.Feedback
import com.reborn.server.domain.feedback.FeedbackRepository
import com.reborn.server.domain.feedback.FeedbackStatus
import com.reborn.server.domain.feedback.dto.FeedbackDto
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.PlaceType
import com.reborn.server.domain.place.UserPlaceMapping
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.global.fcm.FcmClient
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
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class FeedbackServiceTest {

    @Mock
    private lateinit var placeRepository: PlaceRepository

    @Mock
    private lateinit var deviceRepository: DeviceRepository

    @Mock
    private lateinit var feedbackRepository: FeedbackRepository

    @Mock
    private lateinit var userPlaceMappingRepository: UserPlaceMappingRepository

    @Mock
    private lateinit var fcmClient: FcmClient

    @InjectMocks
    private lateinit var feedbackService: FeedbackService

    private lateinit var place: Place
    private lateinit var device: Device
    private lateinit var user: User
    private lateinit var adminMapping: UserPlaceMapping

    @BeforeEach
    fun setUp() {
        place = Place(name = "우리집", qrCode = "qr-uuid", type = PlaceType.HOME, id = 501)
        device = Device(place = place, deviceType = DeviceType.ARDUINO, deviceKey = "arduino_room_01", name = "거실", id = 10)
        user = User(email = "test@reborn.com", name = "테스트", provider = OAuthProvider.GOOGLE, providerId = "google-1", id = 1)
        adminMapping = UserPlaceMapping(user = user, place = place, accessLevel = AccessLevel.ADMIN)
    }

    @Test
    fun `submit - 정상 요청이면 피드백을 저장한다`() {
        val request = FeedbackDto.SubmitRequest(
            qrCode = "qr-uuid",
            deviceId = "arduino_room_01",
            content = "너무 더워요",
            sessionToken = "sess-1",
        )
        val saved = Feedback(device = device, content = "너무 더워요", sessionToken = "sess-1", id = 100).apply { prePersist() }

        given(placeRepository.findByQrCode("qr-uuid")).willReturn(place)
        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(feedbackRepository.existsBySessionToken("sess-1")).willReturn(false)
        given(feedbackRepository.save(any())).willReturn(saved)

        val response = feedbackService.submit(request, "Mozilla/5.0")

        assertThat(response.feedbackId).isEqualTo(100L)
        assertThat(response.status).isEqualTo("PENDING")
    }

    @Test
    fun `submit - ADMIN에게 fcmToken이 있으면 FCM 발송을 호출한다`() {
        val admin = User(email = "admin@reborn.com", name = "관리자", provider = OAuthProvider.GOOGLE, providerId = "google-2", fcmToken = "fcm-token-1", id = 2)
        val mapping = UserPlaceMapping(user = admin, place = place, accessLevel = AccessLevel.ADMIN)
        val request = FeedbackDto.SubmitRequest(qrCode = "qr-uuid", deviceId = "arduino_room_01", content = "덥다", sessionToken = "sess-1")
        val saved = Feedback(device = device, content = "덥다", sessionToken = "sess-1", id = 100).apply { prePersist() }

        given(placeRepository.findByQrCode("qr-uuid")).willReturn(place)
        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(feedbackRepository.existsBySessionToken("sess-1")).willReturn(false)
        given(feedbackRepository.save(any())).willReturn(saved)
        given(userPlaceMappingRepository.findAllByPlaceIdAndAccessLevel(501L, AccessLevel.ADMIN)).willReturn(listOf(mapping))

        feedbackService.submit(request, null)

        verify(fcmClient).send("fcm-token-1", "새로운 피드백이 도착했습니다.", "거실 - 덥다")
    }

    @Test
    fun `submit - content가 없으면 예외가 발생한다`() {
        val request = FeedbackDto.SubmitRequest(qrCode = "qr-uuid", deviceId = "arduino_room_01", content = " ", sessionToken = "sess-1")

        assertThatThrownBy { feedbackService.submit(request, null) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `submit - 존재하지 않는 qrCode면 예외가 발생한다`() {
        val request = FeedbackDto.SubmitRequest(qrCode = "unknown", deviceId = "arduino_room_01", content = "덥다", sessionToken = "sess-1")
        given(placeRepository.findByQrCode("unknown")).willReturn(null)

        assertThatThrownBy { feedbackService.submit(request, null) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `submit - 기기가 다른 장소 소속이면 예외가 발생한다`() {
        val otherPlace = Place(name = "다른집", qrCode = "qr-other", type = PlaceType.HOME, id = 999)
        val otherDevice = Device(place = otherPlace, deviceType = DeviceType.ARDUINO, deviceKey = "arduino_room_01", name = "거실", id = 11)
        val request = FeedbackDto.SubmitRequest(qrCode = "qr-uuid", deviceId = "arduino_room_01", content = "덥다", sessionToken = "sess-1")

        given(placeRepository.findByQrCode("qr-uuid")).willReturn(place)
        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(otherDevice)

        assertThatThrownBy { feedbackService.submit(request, null) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `submit - 동일 세션으로 중복 제출하면 예외가 발생한다`() {
        val request = FeedbackDto.SubmitRequest(qrCode = "qr-uuid", deviceId = "arduino_room_01", content = "덥다", sessionToken = "sess-1")

        given(placeRepository.findByQrCode("qr-uuid")).willReturn(place)
        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(feedbackRepository.existsBySessionToken("sess-1")).willReturn(true)

        assertThatThrownBy { feedbackService.submit(request, null) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS)
    }

    @Test
    fun `getList - ADMIN이면 장소 기준으로 조회한다`() {
        val feedback = Feedback(device = device, content = "덥다", sessionToken = "sess-1", id = 100).apply { prePersist() }
        val page = PageImpl(listOf(feedback))
        val pageable = PageRequest.of(0, 20)

        given(placeRepository.existsById(501L)).willReturn(true)
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(feedbackRepository.findAllByDevice_PlaceId(501L, pageable)).willReturn(page)

        val response = feedbackService.getList(1L, 501L, null, null, pageable)

        assertThat(response.totalCount).isEqualTo(1)
        assertThat(response.feedbacks[0].feedbackId).isEqualTo(100L)
        assertThat(response.feedbacks[0].deviceId).isEqualTo("arduino_room_01")
    }

    @Test
    fun `getList - ADMIN 권한이 없으면 예외가 발생한다`() {
        val pageable = PageRequest.of(0, 20)
        given(placeRepository.existsById(501L)).willReturn(true)
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(null)

        assertThatThrownBy { feedbackService.getList(1L, 501L, null, null, pageable) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.FORBIDDEN)
    }

    @Test
    fun `getList - 잘못된 status 필터면 예외가 발생한다`() {
        val pageable = PageRequest.of(0, 20)
        given(placeRepository.existsById(501L)).willReturn(true)
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)

        assertThatThrownBy { feedbackService.getList(1L, 501L, null, "UNKNOWN", pageable) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `getCount - ADMIN이면 상태별 개수를 집계한다`() {
        given(placeRepository.existsById(501L)).willReturn(true)
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(feedbackRepository.countByDevice_PlaceId(501L)).willReturn(5L)
        given(feedbackRepository.countByDevice_PlaceIdAndStatus(501L, FeedbackStatus.PENDING)).willReturn(2L)
        given(feedbackRepository.countByDevice_PlaceIdAndStatus(501L, FeedbackStatus.APPROVED)).willReturn(2L)
        given(feedbackRepository.countByDevice_PlaceIdAndStatus(501L, FeedbackStatus.REJECTED)).willReturn(1L)

        val response = feedbackService.getCount(1L, 501L)

        assertThat(response.total).isEqualTo(5L)
        assertThat(response.pending).isEqualTo(2L)
        assertThat(response.approved).isEqualTo(2L)
        assertThat(response.rejected).isEqualTo(1L)
    }

    @Test
    fun `updateStatus - PENDING 상태면 APPROVED로 변경한다`() {
        val feedback = Feedback(device = device, content = "덥다", sessionToken = "sess-1", id = 100)
        val request = FeedbackDto.StatusUpdateRequest(status = "APPROVED")

        given(feedbackRepository.findById(100L)).willReturn(Optional.of(feedback))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(placeRepository.existsById(501L)).willReturn(true)

        val response = feedbackService.updateStatus(1L, 100L, request)

        assertThat(response.status).isEqualTo("APPROVED")
    }

    @Test
    fun `updateStatus - 이미 처리된 피드백이면 예외가 발생한다`() {
        val feedback = Feedback(device = device, content = "덥다", sessionToken = "sess-1", status = FeedbackStatus.APPROVED, id = 100)
        val request = FeedbackDto.StatusUpdateRequest(status = "REJECTED")

        given(feedbackRepository.findById(100L)).willReturn(Optional.of(feedback))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(placeRepository.existsById(501L)).willReturn(true)

        assertThatThrownBy { feedbackService.updateStatus(1L, 100L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `updateStatus - 존재하지 않는 피드백이면 예외가 발생한다`() {
        val request = FeedbackDto.StatusUpdateRequest(status = "APPROVED")
        given(feedbackRepository.findById(999L)).willReturn(Optional.empty())

        assertThatThrownBy { feedbackService.updateStatus(1L, 999L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `updateStatus - 잘못된 status 값이면 예외가 발생한다`() {
        val feedback = Feedback(device = device, content = "덥다", sessionToken = "sess-1", id = 100)
        val request = FeedbackDto.StatusUpdateRequest(status = "PENDING")

        given(feedbackRepository.findById(100L)).willReturn(Optional.of(feedback))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(placeRepository.existsById(501L)).willReturn(true)

        assertThatThrownBy { feedbackService.updateStatus(1L, 100L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }
}
