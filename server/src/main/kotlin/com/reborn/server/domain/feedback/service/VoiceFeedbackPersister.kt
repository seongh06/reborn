package com.reborn.server.domain.feedback.service

import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.feedback.Feedback
import com.reborn.server.domain.feedback.FeedbackRepository
import com.reborn.server.domain.feedback.FeedbackSource
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.global.fcm.FcmClient
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// FeedbackService.submitVoice()에서 분리된 짧은 트랜잭션 전용 빈(CodeRabbit 리뷰, PR #144).
// Gemini 오디오 분석/TTS 호출(수십 초까지 걸릴 수 있음)이 DB 트랜잭션 안에서 커넥션을 붙잡고
// 있으면 트래픽이 몰릴 때 커넥션 풀 고갈로 이어질 수 있어, "외부 API 호출은 트랜잭션 밖, DB
// 저장/알림만 짧은 트랜잭션"으로 분리했다. 같은 클래스 내 private @Transactional 메서드는
// self-invocation으로 AOP 프록시가 적용되지 않아 별도 빈으로 뺐다 — deviceId(Long)만 받아
// 이 트랜잭션 안에서 device를 새로 로드해서, 다른 트랜잭션에서 로드된(트랜잭션 종료로 detach된)
// Device의 지연 로딩 필드(place)를 여기서 건드리다 LazyInitializationException이 나는 것도 피한다.
@Service
class VoiceFeedbackPersister(
    private val deviceRepository: DeviceRepository,
    private val feedbackRepository: FeedbackRepository,
    private val userPlaceMappingRepository: UserPlaceMappingRepository,
    private val fcmClient: FcmClient,
) {

    @Transactional
    fun persistAndNotify(deviceId: Long, content: String): Feedback {
        val device = deviceRepository.findById(deviceId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "등록되지 않은 AI 스피커 기기입니다.")
        }

        val feedback = feedbackRepository.save(
            Feedback(device = device, content = content, source = FeedbackSource.VOICE),
        )

        val deviceName = feedback.device?.name ?: device.place.name
        userPlaceMappingRepository.findAllByPlaceIdAndAccessLevel(device.place.id, AccessLevel.ADMIN)
            .mapNotNull { it.user.fcmToken }
            .forEach { token ->
                fcmClient.send(token, "새로운 피드백이 도착했습니다.", "$deviceName - ${feedback.content}")
            }

        return feedback
    }
}
