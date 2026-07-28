package com.reborn.server.domain.feedback.service

import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.feedback.Feedback
import com.reborn.server.domain.feedback.FeedbackRepository
import com.reborn.server.domain.feedback.FeedbackStatus
import com.reborn.server.domain.feedback.client.GeminiClient
import com.reborn.server.domain.feedback.client.GeminiSpeechResult
import com.reborn.server.domain.feedback.client.VoiceTtsCache
import com.reborn.server.domain.feedback.converter.FeedbackConverter
import com.reborn.server.domain.feedback.dto.FeedbackDto
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.global.fcm.FcmClient
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

data class VoiceFeedbackResult(
    val recognized: Boolean,
    val feedbackId: Long?,
    val audio: GeminiSpeechResult,
)

@Service
@Transactional(readOnly = true)
class FeedbackService(
    private val placeRepository: PlaceRepository,
    private val deviceRepository: DeviceRepository,
    private val feedbackRepository: FeedbackRepository,
    private val userPlaceMappingRepository: UserPlaceMappingRepository,
    private val fcmClient: FcmClient,
    private val geminiClient: GeminiClient,
    private val voiceTtsCache: VoiceTtsCache,
    private val voiceFeedbackPersister: VoiceFeedbackPersister,
    private val feedbackAiRecommendationService: FeedbackAiRecommendationService,
) {

    companion object {
        // AI 스피커(#142) 응답 고정 문구 — 2종뿐이라 VoiceTtsCache가 최초 생성 후 재사용한다.
        private const val VOICE_SUCCESS_MESSAGE = "소중한 의견 감사합니다. 잘 전달했어요."
        private const val VOICE_RETRY_MESSAGE = "죄송해요, 잘 듣지 못했어요. 버튼을 다시 누르고 말씀해 주세요."

        // 10분(16kHz*16bit mono)치 WAV보다 넉넉한 상한 — Gemini 호출을 트리거하기 전에
        // 대용량 페이로드를 걸러 비용/메모리 남용을 줄인다(CodeRabbit 리뷰, PR #144).
        private const val MAX_VOICE_AUDIO_BYTES = 10 * 1024 * 1024
    }

    @Transactional
    fun submit(request: FeedbackDto.SubmitRequest, userAgent: String?): FeedbackDto.SubmitResponse {
        val qrCode = request.qrCode?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "qrCode는 필수입니다.")
        val deviceId = request.deviceId?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "deviceId는 필수입니다.")
        val content = request.content?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "피드백 내용은 필수입니다.")
        val sessionToken = request.sessionToken?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "sessionToken은 필수입니다.")

        val place = placeRepository.findByQrCode(qrCode)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        // QR 웹페이지(#163)는 GET /api/feedback/context가 내려준 DB 내부 id로 deviceId를 보낸다 -
        // deviceKey는 기기 자체 인증 비밀값이라 비로그인 공개 API로 노출하지 않기 위함(CodeRabbit 리뷰).
        // deviceKey 문자열을 그대로 보내는 기존 호출부(테스트 등)도 계속 동작하도록 폴백을 둔다.
        val device = (deviceId.toLongOrNull()?.let { deviceRepository.findById(it).orElse(null) }
            ?: deviceRepository.findByDeviceKey(deviceId))
            ?.takeIf { it.place.id == place.id }
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 또는 기기입니다.")

        if (feedbackRepository.existsBySessionToken(sessionToken)) {
            throw BusinessAlertException(CommonErrorCode.TOO_MANY_REQUESTS, "이미 피드백을 제출하셨습니다. 잠시 후 다시 시도해주세요.")
        }

        val feedback = feedbackRepository.save(
            Feedback(device = device, content = content, sessionToken = sessionToken, userAgent = userAgent),
        )

        notifyAdmins(place, feedback)
        scheduleAiRecommendation(feedback.id)

        return FeedbackConverter.toSubmitResponse(feedback)
    }

    // AI 맞춤 피드백 추천은 이 트랜잭션이 커밋된 뒤에만 시작돼야 한다 - 그 전에 비동기로 넘기면
    // FeedbackAiRecommendationService가 findById로 아직 커밋 안 된 row를 못 찾는 레이스가 생김.
    // isSynchronizationActive() 가드는 트랜잭션 매니저가 없는 순수 단위 테스트(Mockito)에서도
    // 이 메서드가 예외 없이 동작하게 하기 위함 - 실제 서비스 환경에선 항상 true.
    private fun scheduleAiRecommendation(feedbackId: Long) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                feedbackAiRecommendationService.generateAndSave(feedbackId)
            }
        })
    }

    // ⚠️ 인증 범위 관련(CodeRabbit 리뷰, PR #144): X-Device-Id 외에 별도 비밀값 검증이 없다는
    // 지적은 유효하지만, AI_SPEAKER는 DeviceType.kt 주석대로 "등록 방식이 ARDUINO와 동일"하도록
    // 의도적으로 설계됐다 — Arduino의 POST /api/metric/collect도 동일하게 deviceId만으로 신뢰하는
    // 하드웨어 기기 모델이라, 이 기기 유형만 appToken 발급/검증(공기계 AEROMETER 방식)을 새로
    // 붙이는 건 이 PR 스코프를 넘는 아키텍처 확장이라 보류. 대신 비용에 직결되는 부분(대용량
    // 페이로드로 Gemini를 반복 호출시키는 남용)은 크기 상한으로 막는다.
    //
    // 트랜잭션 경계(CodeRabbit 리뷰): Gemini 호출(analyzeAudio/TTS, 최대 수십 초)은 DB 트랜잭션
    // 밖에서 수행하고, 실제 저장·알림은 VoiceFeedbackPersister의 짧은 트랜잭션에 위임한다 —
    // 그래서 이 메서드 자체는 클래스 기본(@Transactional(readOnly = true))을 걷어내고
    // NOT_SUPPORTED로 명시한다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun submitVoice(deviceId: String, audioBytes: ByteArray, mimeType: String): VoiceFeedbackResult {
        if (audioBytes.isEmpty()) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "오디오 데이터가 비어있습니다.")
        }
        if (audioBytes.size > MAX_VOICE_AUDIO_BYTES) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "오디오 데이터가 너무 큽니다.")
        }
        val device = deviceRepository.findByDeviceKey(deviceId)
            ?.takeIf { it.deviceType == DeviceType.AI_SPEAKER }
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "등록되지 않은 AI 스피커 기기입니다.")

        val analysis = geminiClient.analyzeAudio(audioBytes, mimeType)

        if (!analysis.recognized || analysis.summary.isBlank()) {
            return VoiceFeedbackResult(
                recognized = false,
                feedbackId = null,
                audio = voiceTtsCache.get(VOICE_RETRY_MESSAGE),
            )
        }

        val feedback = voiceFeedbackPersister.persistAndNotify(device.id, analysis.summary)

        return VoiceFeedbackResult(
            recognized = true,
            feedbackId = feedback.id,
            audio = voiceTtsCache.get(VOICE_SUCCESS_MESSAGE),
        )
    }

    // QR 웹페이지(#163)가 진입 시 장소명 + 제출 대상 기기 목록을 미리 조회한다. SMART_THINGS는
    // 방문자가 직접 지목할 물리 기기가 아니라 클라우드로 제어하는 가전이라 선택지에서 제외한다.
    fun getSubmissionContext(qrCode: String): FeedbackDto.ContextResponse {
        val place = placeRepository.findByQrCode(qrCode)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        val devices = deviceRepository.findAllByPlaceId(place.id)
            .filter { it.deviceType != DeviceType.SMART_THINGS }
        return FeedbackConverter.toContextResponse(place, devices)
    }

    private fun notifyAdmins(place: Place, feedback: Feedback) {
        val deviceName = feedback.device?.name ?: place.name
        userPlaceMappingRepository.findAllByPlaceIdAndAccessLevel(place.id, AccessLevel.ADMIN)
            .mapNotNull { it.user.fcmToken }
            .forEach { token ->
                fcmClient.send(
                    token,
                    "새로운 피드백이 도착했습니다.",
                    "$deviceName - ${feedback.content}",
                    data = mapOf("feedbackId" to feedback.id.toString()),
                )
            }
    }

    fun getList(
        userId: Long,
        placeId: Long,
        deviceId: String?,
        status: String?,
        pageable: Pageable,
    ): FeedbackDto.ListResponse {
        requireAdmin(userId, placeId)
        val statusFilter = parseStatusFilter(status)

        val page: Page<Feedback> = if (deviceId != null) {
            val device = deviceRepository.findByDeviceKey(deviceId)
                ?.takeIf { it.place.id == placeId }
                ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "등록되지 않은 기기입니다.")
            if (statusFilter != null) {
                feedbackRepository.findAllByDeviceIdAndStatus(device.id, statusFilter, pageable)
            } else {
                feedbackRepository.findAllByDeviceId(device.id, pageable)
            }
        } else {
            if (statusFilter != null) {
                feedbackRepository.findAllByDevice_PlaceIdAndStatus(placeId, statusFilter, pageable)
            } else {
                feedbackRepository.findAllByDevice_PlaceId(placeId, pageable)
            }
        }

        return FeedbackConverter.toListResponse(page)
    }

    fun getCount(userId: Long, placeId: Long): FeedbackDto.CountResponse {
        requireAdmin(userId, placeId)

        val total = feedbackRepository.countByDevice_PlaceId(placeId)
        val pending = feedbackRepository.countByDevice_PlaceIdAndStatus(placeId, FeedbackStatus.PENDING)
        val approved = feedbackRepository.countByDevice_PlaceIdAndStatus(placeId, FeedbackStatus.APPROVED)
        val rejected = feedbackRepository.countByDevice_PlaceIdAndStatus(placeId, FeedbackStatus.REJECTED)

        return FeedbackConverter.toCountResponse(total, pending, approved, rejected)
    }

    @Transactional
    fun updateStatus(userId: Long, feedbackId: Long, request: FeedbackDto.StatusUpdateRequest): FeedbackDto.StatusUpdateResponse {
        val feedback = feedbackRepository.findById(feedbackId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 피드백입니다.")
        }
        val placeId = feedback.device?.place?.id
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 피드백입니다.")
        requireAdmin(userId, placeId)

        if (feedback.status != FeedbackStatus.PENDING) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "이미 처리된 피드백입니다.")
        }
        val newStatus = parseTargetStatus(request.status)
        feedback.updateStatus(newStatus)

        return FeedbackConverter.toStatusUpdateResponse(feedback)
    }

    private fun requireAdmin(userId: Long, placeId: Long) {
        if (!placeRepository.existsById(placeId)) {
            throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }
        val mapping = userPlaceMappingRepository.findByUserIdAndPlaceId(userId, placeId)
        if (mapping == null || mapping.accessLevel != AccessLevel.ADMIN) {
            throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "ADMIN 권한이 없습니다.")
        }
    }

    private fun parseStatusFilter(status: String?): FeedbackStatus? {
        if (status.isNullOrBlank()) return null
        return runCatching { FeedbackStatus.valueOf(status) }
            .getOrElse { throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "잘못된 상태 필터 값입니다.") }
    }

    private fun parseTargetStatus(status: String?): FeedbackStatus {
        val parsed = status?.let { runCatching { FeedbackStatus.valueOf(it) }.getOrNull() }
        if (parsed == null || parsed == FeedbackStatus.PENDING) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "잘못된 상태 값입니다.")
        }
        return parsed
    }
}
