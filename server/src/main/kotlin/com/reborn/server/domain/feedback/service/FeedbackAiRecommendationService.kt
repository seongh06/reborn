package com.reborn.server.domain.feedback.service

import com.reborn.server.domain.feedback.FeedbackRepository
import com.reborn.server.domain.feedback.client.GeminiClient
import com.reborn.server.domain.metric.MetricLogRepository
import com.reborn.server.global.async.AsyncConfig
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 피드백 제출 후 "AI 맞춤 피드백"(제출 시점 센서 스냅샷 + 추천 희망 온도)을 비동기로 채운다.
// FeedbackService.submit()의 트랜잭션 커밋 이후에만 호출돼야 한다 - 그 전에 호출하면
// 이 서비스가 findById로 아직 커밋 안 된 feedback row를 못 찾는 레이스가 생김
// (FeedbackService의 TransactionSynchronizationManager.afterCommit 콜백 참고).
@Service
class FeedbackAiRecommendationService(
    private val feedbackRepository: FeedbackRepository,
    private val metricLogRepository: MetricLogRepository,
    private val geminiClient: GeminiClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async(AsyncConfig.ASYNC_EXECUTOR)
    @Transactional
    fun generateAndSave(feedbackId: Long) {
        runCatching {
            val feedback = feedbackRepository.findById(feedbackId).orElse(null) ?: return@runCatching

            // feedback.device(피드백이 지목한 기기)가 아니라 feedback.place 기준으로 찾는다 -
            // AI 스피커처럼 온습도 센서가 없는 기기가 지목되면 예전엔 그 기기 단독 기준으로 찾다가
            // 항상 데이터가 없어 조용히 스킵됐다(#269). 장소 안의 다른 기기(SmartThings/아두이노)
            // 센서값을 대신 참고한다.
            val latestMetric = metricLogRepository.findTopByDevice_PlaceIdOrderByCreatedAtDesc(feedback.place.id)

            // 지금 자동 제어가 실제로 구현된 건 온도(SmartThings 냉난방)뿐이다(#271). "불이 밝아요",
            // "냄새가 나요" 같은 무관한 피드백까지 온도 추천을 계산해서 승인 시 엉뚱한 기기 명령이
            // 나가는 걸 막으면서도(#296), 그냥 스킵하지 않고 사람이 바로 실행할 수 있는 조언을
            // 대신 생성한다 - 승인/거절 버튼은 온도 추천이 있을 때만 노출되므로 조언만 있는
            // 피드백엔 자동 제어를 시도할 방법 자체가 없다.
            if (!isTemperatureRelated(feedback.content)) {
                val advice = geminiClient.recommendAction(
                    feedbackContent = feedback.content,
                    currentTemperature = latestMetric?.temperature,
                    currentHumidity = latestMetric?.humidity,
                ) ?: return@runCatching

                feedback.applyAiAdvice(
                    snapshotTemperature = latestMetric?.temperature,
                    snapshotHumidity = latestMetric?.humidity,
                    snapshotIlluminance = latestMetric?.illuminance,
                    snapshotPeopleCount = latestMetric?.occupancy,
                    advice = advice,
                )
                feedbackRepository.save(feedback)
                return@runCatching
            }

            if (latestMetric == null) {
                log.info("피드백 AI 추천 스킵 - 장소에 센서 데이터 없음: feedbackId={}, placeId={}", feedbackId, feedback.place.id)
                return@runCatching
            }
            val temperature = latestMetric.temperature ?: return@runCatching

            val recommended = geminiClient.recommendTemperatureAdjustment(
                feedbackContent = feedback.content,
                currentTemperature = temperature,
                currentHumidity = latestMetric.humidity,
            ) ?: return@runCatching

            feedback.applyAiRecommendation(
                snapshotTemperature = latestMetric.temperature,
                snapshotHumidity = latestMetric.humidity,
                snapshotIlluminance = latestMetric.illuminance,
                snapshotPeopleCount = latestMetric.occupancy,
                recommendedTemperatureBefore = temperature,
                recommendedTemperatureAfter = recommended,
            )
            feedbackRepository.save(feedback)
        }.onFailure { e -> log.warn("피드백 AI 추천 생성 실패: feedbackId={}, error={}", feedbackId, e.message) }
    }

    // core:ui의 classifyFeedbackType()과 동일한 키워드 기준(더워요/추워요 계열만) - 서버는 Compose
    // 모듈을 의존할 수 없어 판정 로직만 최소한으로 복제. 두 곳 중 하나를 바꾸면 다른 쪽도 확인할 것.
    private fun isTemperatureRelated(content: String): Boolean =
        content.contains("덥") || content.contains("더워") || content.contains("춥") || content.contains("추워")
}
