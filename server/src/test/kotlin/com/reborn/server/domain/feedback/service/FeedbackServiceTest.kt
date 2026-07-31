package com.reborn.server.domain.feedback.service

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.auth.OAuthProvider
import com.reborn.server.domain.auth.User
import com.reborn.server.domain.feedback.Feedback
import com.reborn.server.domain.feedback.FeedbackRepository
import com.reborn.server.domain.feedback.FeedbackStatus
import com.reborn.server.domain.feedback.client.GeminiClient
import com.reborn.server.domain.feedback.client.VoiceTtsCache
import com.reborn.server.domain.feedback.dto.FeedbackDto
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.PlaceType
import com.reborn.server.domain.place.UserPlaceMapping
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.domain.smartthings.service.SmartThingsDeviceService
import com.reborn.server.global.fcm.FcmClient
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import com.reborn.server.global.redis.RedisUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Duration
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

    @Mock
    private lateinit var geminiClient: GeminiClient

    @Mock
    private lateinit var voiceTtsCache: VoiceTtsCache

    @Mock
    private lateinit var voiceFeedbackPersister: VoiceFeedbackPersister

    @Mock
    private lateinit var feedbackAiRecommendationService: FeedbackAiRecommendationService

    @Mock
    private lateinit var smartThingsDeviceService: SmartThingsDeviceService

    @Mock
    private lateinit var redisUtil: RedisUtil

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

    // FeedbackService.submit()의 dedupKey 생성식과 항상 동일하게 유지해야 함
    private fun dedupKey(qrCode: String, deviceId: String?, content: String): String =
        "feedback:dedup:$qrCode:${deviceId ?: "none"}:${content.hashCode()}"

    @Test
    fun `submit - 정상 요청이면 피드백을 저장한다`() {
        val request = FeedbackDto.SubmitRequest(
            qrCode = "qr-uuid",
            deviceId = "arduino_room_01",
            content = "너무 더워요",
            sessionToken = "sess-1",
        )
        val saved =
            Feedback(device = device, place = place, content = "너무 더워요", sessionToken = "sess-1", id = 100)
                .apply { prePersist() }

        given(placeRepository.findByQrCode("qr-uuid")).willReturn(place)
        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(redisUtil.setIfAbsent(dedupKey("qr-uuid", "arduino_room_01", "너무 더워요"), "1", Duration.ofSeconds(10)))
            .willReturn(true)
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
        val saved =
            Feedback(device = device, place = place, content = "덥다", sessionToken = "sess-1", id = 100)
                .apply { prePersist() }

        given(placeRepository.findByQrCode("qr-uuid")).willReturn(place)
        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(redisUtil.setIfAbsent(dedupKey("qr-uuid", "arduino_room_01", "덥다"), "1", Duration.ofSeconds(10)))
            .willReturn(true)
        given(feedbackRepository.save(any())).willReturn(saved)
        given(userPlaceMappingRepository.findAllByPlaceIdAndAccessLevel(501L, AccessLevel.ADMIN)).willReturn(listOf(mapping))

        feedbackService.submit(request, null)

        verify(fcmClient).send(
            "fcm-token-1",
            "새로운 피드백이 도착했습니다.",
            "거실 - 덥다",
            mapOf("feedbackId" to "100"),
        )
    }

    @Test
    fun `submit - deviceId가 없어도 피드백을 저장한다`() {
        // feedback.html은 이 장소에 기기가 없거나 사용자가 기기를 지목하지 않으면
        // deviceId 없이 제출한다(Feedback.device가 nullable인 이유) - 이 요청을 서버가
        // 거부하면 안 된다는 회귀 방지 테스트.
        val request = FeedbackDto.SubmitRequest(
            qrCode = "qr-uuid",
            deviceId = null,
            content = "너무 더워요",
            sessionToken = "sess-1",
        )
        val saved =
            Feedback(device = null, place = place, content = "너무 더워요", sessionToken = "sess-1", id = 100)
                .apply { prePersist() }

        given(placeRepository.findByQrCode("qr-uuid")).willReturn(place)
        given(redisUtil.setIfAbsent(dedupKey("qr-uuid", null, "너무 더워요"), "1", Duration.ofSeconds(10)))
            .willReturn(true)
        given(feedbackRepository.save(any())).willReturn(saved)

        val response = feedbackService.submit(request, "Mozilla/5.0")

        assertThat(response.feedbackId).isEqualTo(100L)
        verify(deviceRepository, never()).findById(anyLong())
        verify(deviceRepository, never()).findByDeviceKey(anyString())

        val savedCaptor = ArgumentCaptor.forClass(Feedback::class.java)
        verify(feedbackRepository).save(savedCaptor.capture())
        assertThat(savedCaptor.value.device).isNull()
        assertThat(savedCaptor.value.place).isEqualTo(place)
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
    fun `getSubmissionContext - SmartThings 기기가 있으면 hasControllableDevice가 true다`() {
        val smartThingsDevice = Device(place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "st-1", name = "에어컨", id = 20)
        given(placeRepository.findByQrCode("qr-uuid")).willReturn(place)
        given(deviceRepository.findAllByPlaceId(501L)).willReturn(listOf(device, smartThingsDevice))

        val response = feedbackService.getSubmissionContext("qr-uuid")

        assertThat(response.hasControllableDevice).isTrue()
        // SmartThings는 방문자가 직접 지목할 대상이 아니라 목록에서 제외됨(기존 동작)
        assertThat(response.devices).extracting("deviceId").containsExactly(device.id.toString())
    }

    @Test
    fun `getSubmissionContext - SmartThings 기기가 없으면 hasControllableDevice가 false다`() {
        given(placeRepository.findByQrCode("qr-uuid")).willReturn(place)
        given(deviceRepository.findAllByPlaceId(501L)).willReturn(listOf(device))

        val response = feedbackService.getSubmissionContext("qr-uuid")

        assertThat(response.hasControllableDevice).isFalse()
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
    fun `submit - 짧은 시간 내 동일한 장소·기기·내용으로 재전송하면 예외가 발생한다`() {
        val request = FeedbackDto.SubmitRequest(qrCode = "qr-uuid", deviceId = "arduino_room_01", content = "덥다", sessionToken = "sess-1")

        given(placeRepository.findByQrCode("qr-uuid")).willReturn(place)
        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(redisUtil.setIfAbsent(dedupKey("qr-uuid", "arduino_room_01", "덥다"), "1", Duration.ofSeconds(10)))
            .willReturn(false)

        assertThatThrownBy { feedbackService.submit(request, null) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS)
    }

    @Test
    fun `submit - sessionToken 없이도 피드백을 저장한다`() {
        // #261 - sessionToken은 더 이상 중복 방지에 쓰이지 않는 참고용 optional 값이라
        // 없어도 제출이 거부되면 안 된다는 회귀 방지 테스트.
        val request = FeedbackDto.SubmitRequest(qrCode = "qr-uuid", deviceId = "arduino_room_01", content = "덥다", sessionToken = null)
        val saved = Feedback(device = device, place = place, content = "덥다", sessionToken = null, id = 100)
            .apply { prePersist() }

        given(placeRepository.findByQrCode("qr-uuid")).willReturn(place)
        given(deviceRepository.findByDeviceKey("arduino_room_01")).willReturn(device)
        given(redisUtil.setIfAbsent(dedupKey("qr-uuid", "arduino_room_01", "덥다"), "1", Duration.ofSeconds(10)))
            .willReturn(true)
        given(feedbackRepository.save(any())).willReturn(saved)

        val response = feedbackService.submit(request, null)

        assertThat(response.feedbackId).isEqualTo(100L)
    }

    @Test
    fun `getList - ADMIN이면 장소 기준으로 조회한다`() {
        val feedback =
            Feedback(device = device, place = place, content = "덥다", sessionToken = "sess-1", id = 100)
                .apply { prePersist() }
        val page = PageImpl(listOf(feedback))
        val pageable = PageRequest.of(0, 20)

        given(placeRepository.existsById(501L)).willReturn(true)
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(feedbackRepository.findAllByPlaceId(501L, pageable)).willReturn(page)

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
        given(feedbackRepository.countByPlaceId(501L)).willReturn(5L)
        given(feedbackRepository.countByPlaceIdAndStatus(501L, FeedbackStatus.PENDING)).willReturn(2L)
        given(feedbackRepository.countByPlaceIdAndStatus(501L, FeedbackStatus.APPROVED)).willReturn(2L)
        given(feedbackRepository.countByPlaceIdAndStatus(501L, FeedbackStatus.REJECTED)).willReturn(1L)

        val response = feedbackService.getCount(1L, 501L)

        assertThat(response.total).isEqualTo(5L)
        assertThat(response.pending).isEqualTo(2L)
        assertThat(response.approved).isEqualTo(2L)
        assertThat(response.rejected).isEqualTo(1L)
    }

    @Test
    fun `updateStatus - PENDING 상태면 APPROVED로 변경한다`() {
        val feedback = Feedback(device = device, place = place, content = "덥다", sessionToken = "sess-1", id = 100)
        val request = FeedbackDto.StatusUpdateRequest(status = "APPROVED")

        given(feedbackRepository.findById(100L)).willReturn(Optional.of(feedback))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(placeRepository.existsById(501L)).willReturn(true)

        val response = feedbackService.updateStatus(1L, 100L, request)

        assertThat(response.status).isEqualTo("APPROVED")
    }

    @Test
    fun `updateStatus - 승인 시 추천 온도와 SmartThings 기기가 있으면 제어 명령을 전송한다`() {
        val feedback = Feedback(device = device, place = place, content = "덥다", sessionToken = "sess-1", id = 100)
        feedback.applyAiRecommendation(
            snapshotTemperature = 28.0, snapshotHumidity = 60.0, snapshotIlluminance = null, snapshotPeopleCount = null,
            recommendedTemperatureBefore = 28.0, recommendedTemperatureAfter = 24.6,
        )
        val smartThingsDevice = Device(
            place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "st-1", name = "에어컨", id = 20,
        )
        val request = FeedbackDto.StatusUpdateRequest(status = "APPROVED")

        given(feedbackRepository.findById(100L)).willReturn(Optional.of(feedback))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(placeRepository.existsById(501L)).willReturn(true)
        given(deviceRepository.findAllByPlaceIdAndDeviceType(501L, DeviceType.SMART_THINGS)).willReturn(listOf(smartThingsDevice))

        val response = feedbackService.updateStatus(1L, 100L, request)

        assertThat(response.controlSent).isTrue()
        verify(smartThingsDeviceService).controlInternal(
            smartThingsDevice,
            com.reborn.server.domain.device.dto.DeviceDto.ControlRequest(temperature = 25),
        )
    }

    @Test
    fun `updateStatus - SmartThings 기기가 여러 대면 대상을 특정할 수 없어 제어를 건너뛴다`() {
        val feedback = Feedback(device = device, place = place, content = "덥다", sessionToken = "sess-1", id = 100)
        feedback.applyAiRecommendation(
            snapshotTemperature = 28.0, snapshotHumidity = 60.0, snapshotIlluminance = null, snapshotPeopleCount = null,
            recommendedTemperatureBefore = 28.0, recommendedTemperatureAfter = 24.6,
        )
        val smartThingsDevice1 = Device(
            place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "st-1", name = "에어컨", id = 20,
        )
        val smartThingsDevice2 = Device(
            place = place, deviceType = DeviceType.SMART_THINGS, deviceKey = "st-2", name = "거실 에어컨", id = 21,
        )
        val request = FeedbackDto.StatusUpdateRequest(status = "APPROVED")

        given(feedbackRepository.findById(100L)).willReturn(Optional.of(feedback))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(placeRepository.existsById(501L)).willReturn(true)
        given(deviceRepository.findAllByPlaceIdAndDeviceType(501L, DeviceType.SMART_THINGS))
            .willReturn(listOf(smartThingsDevice1, smartThingsDevice2))

        val response = feedbackService.updateStatus(1L, 100L, request)

        assertThat(response.controlSent).isFalse()
        verifyNoInteractions(smartThingsDeviceService)
    }

    @Test
    fun `updateStatus - 승인해도 SmartThings 기기가 없으면 제어를 건너뛴다`() {
        val feedback = Feedback(device = device, place = place, content = "덥다", sessionToken = "sess-1", id = 100)
        feedback.applyAiRecommendation(
            snapshotTemperature = 28.0, snapshotHumidity = 60.0, snapshotIlluminance = null, snapshotPeopleCount = null,
            recommendedTemperatureBefore = 28.0, recommendedTemperatureAfter = 24.6,
        )
        val request = FeedbackDto.StatusUpdateRequest(status = "APPROVED")

        given(feedbackRepository.findById(100L)).willReturn(Optional.of(feedback))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(placeRepository.existsById(501L)).willReturn(true)
        given(deviceRepository.findAllByPlaceIdAndDeviceType(501L, DeviceType.SMART_THINGS)).willReturn(emptyList())

        val response = feedbackService.updateStatus(1L, 100L, request)

        assertThat(response.controlSent).isFalse()
    }

    @Test
    fun `updateStatus - 추천 온도가 없으면 SmartThings 기기 조회 없이 건너뛴다`() {
        val feedback = Feedback(device = device, place = place, content = "덥다", sessionToken = "sess-1", id = 100)
        val request = FeedbackDto.StatusUpdateRequest(status = "APPROVED")

        given(feedbackRepository.findById(100L)).willReturn(Optional.of(feedback))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(placeRepository.existsById(501L)).willReturn(true)

        val response = feedbackService.updateStatus(1L, 100L, request)

        assertThat(response.controlSent).isFalse()
        verifyNoInteractions(deviceRepository)
    }

    @Test
    fun `updateStatus - 거절 시에는 제어를 시도하지 않는다`() {
        val feedback = Feedback(device = device, place = place, content = "덥다", sessionToken = "sess-1", id = 100)
        feedback.applyAiRecommendation(
            snapshotTemperature = 28.0, snapshotHumidity = 60.0, snapshotIlluminance = null, snapshotPeopleCount = null,
            recommendedTemperatureBefore = 28.0, recommendedTemperatureAfter = 24.6,
        )
        val request = FeedbackDto.StatusUpdateRequest(status = "REJECTED")

        given(feedbackRepository.findById(100L)).willReturn(Optional.of(feedback))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(placeRepository.existsById(501L)).willReturn(true)

        val response = feedbackService.updateStatus(1L, 100L, request)

        assertThat(response.controlSent).isFalse()
        verifyNoInteractions(deviceRepository)
    }

    @Test
    fun `updateStatus - 이미 처리된 피드백이면 예외가 발생한다`() {
        val feedback = Feedback(
            device = device, place = place, content = "덥다", sessionToken = "sess-1",
            status = FeedbackStatus.APPROVED, id = 100,
        )
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
        val feedback = Feedback(device = device, place = place, content = "덥다", sessionToken = "sess-1", id = 100)
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
