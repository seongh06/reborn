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
import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.domain.smartthings.service.SmartThingsDeviceService
import com.reborn.server.global.fcm.FcmClient
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import com.reborn.server.global.redis.RedisUtil
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.io.File
import java.time.Duration
import kotlin.math.roundToInt

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
    private val smartThingsDeviceService: SmartThingsDeviceService,
    private val redisUtil: RedisUtil,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        // AI 스피커(#142) 응답 고정 문구 — 2종뿐이라 VoiceTtsCache가 최초 생성 후 재사용한다.
        private const val VOICE_SUCCESS_MESSAGE = "피드백이 접수되었습니다. 소중한 의견 감사합니다."
        private const val VOICE_RETRY_MESSAGE = "죄송해요, 잘 듣지 못했어요. 버튼을 다시 누르고 말씀해 주세요."

        // 10분(16kHz*16bit mono)치 WAV보다 넉넉한 상한 — Gemini 호출을 트리거하기 전에
        // 대용량 페이로드를 걸러 비용/메모리 남용을 줄인다(CodeRabbit 리뷰, PR #144).
        private const val MAX_VOICE_AUDIO_BYTES = 10 * 1024 * 1024

        // 중복 클릭/새로고침 재전송만 막을 정도의 짧은 창(#261) - sessionToken 영구 유니크
        // 방식은 같은 브라우저가 그 장소에 영원히 재제출 못 하는 버그였다.
        private val DUPLICATE_SUBMIT_WINDOW = Duration.ofSeconds(10)
    }

    @Transactional
    fun submit(request: FeedbackDto.SubmitRequest, userAgent: String?): FeedbackDto.SubmitResponse {
        val qrCode = request.qrCode?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "qrCode는 필수입니다.")
        val deviceId = request.deviceId?.takeIf { it.isNotBlank() }
        val content = request.content?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "피드백 내용은 필수입니다.")
        val sessionToken = request.sessionToken?.takeIf { it.isNotBlank() }

        val place = placeRepository.findByQrCode(qrCode)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        // deviceId는 선택값 - QR 웹페이지(feedback.html)는 이 장소에 등록된 기기가 없거나 사용자가
        // 특정 기기를 지목하지 않으면 deviceId 없이도 제출 가능하도록 만들어져 있다(Feedback.device가
        // nullable인 이유). deviceId가 오면 기존처럼 해당 장소 소속 기기인지 검증한다.
        // QR 웹페이지(#163)는 GET /api/feedback/context가 내려준 DB 내부 id로 deviceId를 보낸다 -
        // deviceKey는 기기 자체 인증 비밀값이라 비로그인 공개 API로 노출하지 않기 위함(CodeRabbit 리뷰).
        // deviceKey 문자열을 그대로 보내는 기존 호출부(테스트 등)도 계속 동작하도록 폴백을 둔다.
        val device = deviceId?.let { id ->
            (id.toLongOrNull()?.let { deviceRepository.findById(it).orElse(null) }
                ?: deviceRepository.findByDeviceKey(id))
                ?.takeIf { it.place.id == place.id }
                ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 또는 기기입니다.")
        }

        // sessionToken 영구 유니크 제약(#261)을 대체 - QR 페이지가 브라우저
        // localStorage에 토큰을 영구 저장/재사용해서, 한 번 제출하면 그 브라우저에서 같은 장소에
        // 영원히 다시 못 보내는 버그가 있었다. 대신 (장소+기기+내용) 조합으로 짧은 시간(중복
        // 클릭/새로고침 재전송 방지 목적)만 막고, 그 시간이 지나거나 내용이 다르면 항상 제출 가능.
        val dedupKey = "feedback:dedup:$qrCode:${deviceId ?: "none"}:${content.hashCode()}"
        if (!redisUtil.setIfAbsent(dedupKey, "1", DUPLICATE_SUBMIT_WINDOW)) {
            throw BusinessAlertException(CommonErrorCode.TOO_MANY_REQUESTS, "이미 피드백을 제출하셨습니다. 잠시 후 다시 시도해주세요.")
        }

        val feedback = feedbackRepository.save(
            Feedback(device = device, place = place, content = content, sessionToken = sessionToken, userAgent = userAgent),
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

        // 임시 디버그용 - 실기기 테스트하면서 실제로 어떤 오디오가 녹음/전송되는지 확인하려고
        // 컨테이너 내부(퍼블릭 서빙 경로 아님, 재배포 전까지만 유지됨)에 원본을 저장한다.
        // 디버깅 끝나면 제거할 것.
        runCatching {
            val debugDir = File("/tmp/voice-debug").apply { mkdirs() }
            val fileName = "${System.currentTimeMillis()}_${deviceId}.wav"
            File(debugDir, fileName).writeBytes(audioBytes)
            log.info("음성 피드백 디버그 저장: /tmp/voice-debug/{}", fileName)
        }.onFailure { log.warn("음성 피드백 디버그 저장 실패: {}", it.message) }

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

    // 임시 디버그용 - 실기기 스피커 볼륨(SPEAKER_GAIN) 조정할 때, 녹음→분석 전체 흐름 없이
    // 고정 문구 TTS만 빠르게 반복 재생해서 테스트할 수 있게 한다. 디버깅 끝나면 제거할 것.
    // 짧은 인사말("안녕하세요.")은 Gemini TTS 모델이 오디오 대신 대화 응답(텍스트)을 생성하려다
    // 400을 반환하는 경우가 있어서(모델이 "인사에 답해야 하는 상황"으로 오인) 실제 프로덕션에서
    // 이미 검증된 문구를 그대로 재사용한다 - 볼륨 테스트도 실사용과 동일한 음성이라 더 대표성 있음.
    fun getTestTts(): GeminiSpeechResult = voiceTtsCache.get(VOICE_SUCCESS_MESSAGE)

    // QR 웹페이지(#163)가 진입 시 장소명 + 제출 대상 기기 목록을 미리 조회한다. SMART_THINGS는
    // 방문자가 직접 지목할 물리 기기가 아니라 클라우드로 제어하는 가전이라 선택지에서 제외한다.
    fun getSubmissionContext(qrCode: String): FeedbackDto.ContextResponse {
        val place = placeRepository.findByQrCode(qrCode)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        val allDevices = deviceRepository.findAllByPlaceId(place.id)
        val devices = allDevices.filter { it.deviceType != DeviceType.SMART_THINGS }
        val hasControllableDevice = allDevices.any { it.deviceType == DeviceType.SMART_THINGS }
        return FeedbackConverter.toContextResponse(place, devices, hasControllableDevice)
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
                feedbackRepository.findAllByPlaceIdAndStatus(placeId, statusFilter, pageable)
            } else {
                feedbackRepository.findAllByPlaceId(placeId, pageable)
            }
        }

        return FeedbackConverter.toListResponse(page)
    }

    fun getCount(userId: Long, placeId: Long): FeedbackDto.CountResponse {
        requireAdmin(userId, placeId)

        val total = feedbackRepository.countByPlaceId(placeId)
        val pending = feedbackRepository.countByPlaceIdAndStatus(placeId, FeedbackStatus.PENDING)
        val approved = feedbackRepository.countByPlaceIdAndStatus(placeId, FeedbackStatus.APPROVED)
        val rejected = feedbackRepository.countByPlaceIdAndStatus(placeId, FeedbackStatus.REJECTED)

        return FeedbackConverter.toCountResponse(total, pending, approved, rejected)
    }

    @Transactional
    fun updateStatus(userId: Long, feedbackId: Long, request: FeedbackDto.StatusUpdateRequest): FeedbackDto.StatusUpdateResponse {
        val feedback = feedbackRepository.findById(feedbackId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 피드백입니다.")
        }
        requireAdmin(userId, feedback.place.id)

        if (feedback.status != FeedbackStatus.PENDING) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "이미 처리된 피드백입니다.")
        }
        val newStatus = parseTargetStatus(request.status)
        feedback.updateStatus(newStatus)

        val controlSent = if (newStatus == FeedbackStatus.APPROVED) {
            sendApprovedTemperatureControl(feedback)
        } else {
            false
        }

        return FeedbackConverter.toStatusUpdateResponse(feedback, controlSent)
    }

    // 승인 시 "AI 맞춤 피드백"의 추천 희망 온도를 이 장소의 SmartThings 기기로 즉시 전송한다.
    // 장소에 SmartThings 기기가 없거나 추천값 자체가 없으면(Gemini 미설정/실패 등) 조용히
    // 건너뛴다 - 이건 정상적인 상태고 승인 자체를 막을 이유가 아니다. 반면 기기는 있는데
    // 실제 전송이 실패하면(SmartThings 토큰 만료 등) 예외를 그대로 던져 트랜잭션을 롤백한다 -
    // "승인을 누르면 바로 전송됩니다"라는 화면 문구를 실제로 지키려면 전송 실패 시 승인
    // 자체도 실패해야 관리자가 다시 시도할 수 있다.
    private fun sendApprovedTemperatureControl(feedback: Feedback): Boolean {
        val recommendedTemperature = feedback.recommendedTemperatureAfter ?: return false
        val smartThingsDevices = deviceRepository
            .findAllByPlaceIdAndDeviceType(feedback.place.id, DeviceType.SMART_THINGS)

        // 장소에 SmartThings 기기가 2개 이상이면 "어느 기기가 온도 조절 대상인지" 알 방법이
        // 현재 없다(장소당 1개의 에어컨을 가정한 설계) - 임의로 하나를 골라 엉뚱한 기기에 명령을
        // 보내는 대신 건너뛴다(CodeRabbit 리뷰). 여러 대를 구분해서 제어하려면 추천 대상 기기를
        // Feedback에 명시적으로 저장하는 구조 변경이 필요함 - 향후 과제.
        if (smartThingsDevices.size > 1) {
            log.warn(
                "피드백 승인 - SmartThings 기기가 여러 대라 대상을 특정할 수 없어 제어를 건너뜀: feedbackId={}, placeId={}, deviceCount={}",
                feedback.id, feedback.place.id, smartThingsDevices.size,
            )
            return false
        }
        val targetDevice = smartThingsDevices.firstOrNull() ?: return false

        smartThingsDeviceService.controlInternal(
            targetDevice,
            DeviceDto.ControlRequest(temperature = recommendedTemperature.roundToInt()),
        )
        log.info(
            "피드백 승인 - SmartThings 제어 전송: feedbackId={}, targetDeviceId={}, temperature={}",
            feedback.id, targetDevice.id, recommendedTemperature,
        )
        return true
    }

    // 승인/거절(status)과는 별도 축이라 이미 처리된 피드백이든 아니든, 이미 읽었든 아니든 항상
    // 성공한다(멱등) - updateStatus()의 "이미 처리된 피드백" 가드를 재사용하지 않는다.
    @Transactional
    fun markRead(userId: Long, feedbackId: Long) {
        val feedback = feedbackRepository.findById(feedbackId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 피드백입니다.")
        }
        requireAdmin(userId, feedback.place.id)
        feedback.markRead()
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
