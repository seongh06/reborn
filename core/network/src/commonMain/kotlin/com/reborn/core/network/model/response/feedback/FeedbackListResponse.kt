package com.reborn.core.network.model.response.feedback

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackListResponse(
    val totalCount: Long,
    val feedbacks: List<FeedbackItemResponse>,
)

@Serializable
data class FeedbackItemResponse(
    val feedbackId: Long,
    val deviceId: String,
    val deviceName: String?,
    val content: String,
    val source: String,
    val status: String,
    val createdAt: String,
    // "AI 맞춤 피드백" - 제출 직후 비동기로 채워지므로 접수 직후 잠깐은 전부 null일 수 있음
    val snapshotTemperature: Double? = null,
    val snapshotHumidity: Double? = null,
    val snapshotIlluminance: Int? = null,
    val snapshotPeopleCount: Int? = null,
    val recommendedTemperatureBefore: Double? = null,
    val recommendedTemperatureAfter: Double? = null,
    // IoT 제어 대상이 아닌 피드백에 대한 AI 조언(#296) - recommendedTemperature*와 상호 배타적.
    val aiAdvice: String? = null,
)
