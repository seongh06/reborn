package com.reborn.server.domain.feedback.service

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.feedback.Feedback
import com.reborn.server.domain.feedback.FeedbackRepository
import com.reborn.server.domain.feedback.client.GeminiClient
import com.reborn.server.domain.metric.MetricLog
import com.reborn.server.domain.metric.MetricLogRepository
import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.PlaceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class FeedbackAiRecommendationServiceTest {

    @Mock
    private lateinit var feedbackRepository: FeedbackRepository

    @Mock
    private lateinit var metricLogRepository: MetricLogRepository

    @Mock
    private lateinit var geminiClient: GeminiClient

    private lateinit var service: FeedbackAiRecommendationService

    private lateinit var place: Place
    private lateinit var device: Device

    @BeforeEach
    fun setUp() {
        place = Place(name = "테스트 거실", qrCode = "qr-test", type = PlaceType.HOME, id = 501)
        device = Device(place = place, deviceType = DeviceType.ARDUINO, deviceKey = "arduino_room_01", name = "거실", id = 10)
        service = FeedbackAiRecommendationService(feedbackRepository, metricLogRepository, geminiClient)
    }

    @Test
    fun `generateAndSave - 최신 메트릭과 Gemini 응답이 있으면 피드백에 추천값을 저장한다`() {
        val feedback = Feedback(device = device, place = place, content = "너무 더워요", sessionToken = "sess-1", id = 100)
        val latestMetric = MetricLog(device = device, temperature = 28.0, humidity = 60.0, illuminance = 300, occupancy = 2, id = 1)

        given(feedbackRepository.findById(100L)).willReturn(Optional.of(feedback))
        given(metricLogRepository.findTopByDeviceIdOrderByCreatedAtDesc(10L)).willReturn(latestMetric)
        given(geminiClient.recommendTemperatureAdjustment("너무 더워요", 28.0, 60.0)).willReturn(25.0)

        service.generateAndSave(100L)

        assertThat(feedback.snapshotTemperature).isEqualTo(28.0)
        assertThat(feedback.snapshotHumidity).isEqualTo(60.0)
        assertThat(feedback.snapshotIlluminance).isEqualTo(300)
        assertThat(feedback.snapshotPeopleCount).isEqualTo(2)
        assertThat(feedback.recommendedTemperatureBefore).isEqualTo(28.0)
        assertThat(feedback.recommendedTemperatureAfter).isEqualTo(25.0)
        verify(feedbackRepository).save(feedback)
    }

    @Test
    fun `generateAndSave - 최신 메트릭이 없으면 아무 것도 하지 않는다`() {
        val feedback = Feedback(device = device, place = place, content = "너무 더워요", sessionToken = "sess-1", id = 100)
        given(feedbackRepository.findById(100L)).willReturn(Optional.of(feedback))
        given(metricLogRepository.findTopByDeviceIdOrderByCreatedAtDesc(10L)).willReturn(null)

        service.generateAndSave(100L)

        assertThat(feedback.recommendedTemperatureAfter).isNull()
        verify(feedbackRepository, never()).save(feedback)
    }

    @Test
    fun `generateAndSave - Gemini가 추천을 못 만들면 저장하지 않는다`() {
        val feedback = Feedback(device = device, place = place, content = "너무 더워요", sessionToken = "sess-1", id = 100)
        val latestMetric = MetricLog(device = device, temperature = 28.0, humidity = 60.0, id = 1)

        given(feedbackRepository.findById(100L)).willReturn(Optional.of(feedback))
        given(metricLogRepository.findTopByDeviceIdOrderByCreatedAtDesc(10L)).willReturn(latestMetric)
        given(geminiClient.recommendTemperatureAdjustment("너무 더워요", 28.0, 60.0)).willReturn(null)

        service.generateAndSave(100L)

        assertThat(feedback.recommendedTemperatureAfter).isNull()
        verify(feedbackRepository, never()).save(feedback)
    }

    @Test
    fun `generateAndSave - 존재하지 않는 피드백이면 아무 것도 하지 않는다`() {
        given(feedbackRepository.findById(999L)).willReturn(Optional.empty())

        service.generateAndSave(999L)

        verifyNoInteractions(metricLogRepository, geminiClient)
    }

    @Test
    fun `generateAndSave - Gemini 호출이 예외를 던져도 전파하지 않는다`() {
        val feedback = Feedback(device = device, place = place, content = "너무 더워요", sessionToken = "sess-1", id = 100)
        val latestMetric = MetricLog(device = device, temperature = 28.0, humidity = 60.0, id = 1)

        given(feedbackRepository.findById(100L)).willReturn(Optional.of(feedback))
        given(metricLogRepository.findTopByDeviceIdOrderByCreatedAtDesc(10L)).willReturn(latestMetric)
        given(geminiClient.recommendTemperatureAdjustment("너무 더워요", 28.0, 60.0))
            .willThrow(RuntimeException("Gemini 오류"))

        service.generateAndSave(100L)

        assertThat(feedback.recommendedTemperatureAfter).isNull()
    }
}
