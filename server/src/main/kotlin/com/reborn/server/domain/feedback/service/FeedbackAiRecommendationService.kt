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
            val device = feedback.device ?: return@runCatching
            val latestMetric = metricLogRepository.findTopByDeviceIdOrderByCreatedAtDesc(device.id) ?: return@runCatching
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
}
