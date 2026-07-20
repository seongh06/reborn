package com.reborn.server.domain.feedback.service

import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.feedback.Feedback
import com.reborn.server.domain.feedback.FeedbackRepository
import com.reborn.server.domain.feedback.FeedbackSource
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
import org.springframework.transaction.annotation.Transactional

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
) {

    companion object {
        // AI 스피커(#142) 응답 고정 문구 — 2종뿐이라 VoiceTtsCache가 최초 생성 후 재사용한다.
        private const val VOICE_SUCCESS_MESSAGE = "소중한 의견 감사합니다. 잘 전달했어요."
        private const val VOICE_RETRY_MESSAGE = "죄송해요, 잘 듣지 못했어요. 버튼을 다시 누르고 말씀해 주세요."
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
        val device = deviceRepository.findByDeviceKey(deviceId)
            ?.takeIf { it.place.id == place.id }
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 또는 기기입니다.")

        if (feedbackRepository.existsBySessionToken(sessionToken)) {
            throw BusinessAlertException(CommonErrorCode.TOO_MANY_REQUESTS, "이미 피드백을 제출하셨습니다. 잠시 후 다시 시도해주세요.")
        }

        val feedback = feedbackRepository.save(
            Feedback(device = device, content = content, sessionToken = sessionToken, userAgent = userAgent),
        )

        notifyAdmins(place, feedback)

        return FeedbackConverter.toSubmitResponse(feedback)
    }

    @Transactional
    fun submitVoice(deviceId: String, audioBytes: ByteArray, mimeType: String): VoiceFeedbackResult {
        if (audioBytes.isEmpty()) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "오디오 데이터가 비어있습니다.")
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

        val feedback = feedbackRepository.save(
            Feedback(device = device, content = analysis.summary, source = FeedbackSource.VOICE),
        )
        notifyAdmins(device.place, feedback)

        return VoiceFeedbackResult(
            recognized = true,
            feedbackId = feedback.id,
            audio = voiceTtsCache.get(VOICE_SUCCESS_MESSAGE),
        )
    }

    private fun notifyAdmins(place: Place, feedback: Feedback) {
        val deviceName = feedback.device?.name ?: place.name
        userPlaceMappingRepository.findAllByPlaceIdAndAccessLevel(place.id, AccessLevel.ADMIN)
            .mapNotNull { it.user.fcmToken }
            .forEach { token ->
                fcmClient.send(token, "새로운 피드백이 도착했습니다.", "$deviceName - ${feedback.content}")
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
