package com.reborn.core.model

data class Feedback(
    val feedbackId: Long,
    val deviceName: String?,
    val content: String,
    val status: String,
    // ISO-8601 로컬 날짜시각(타임존 없음, 서버 LocalDateTime 그대로) - 표시용 포맷팅은 UI 레이어 책임
    val createdAt: String,
    // "AI 맞춤 피드백" - 제출 직후 서버가 비동기로 채우므로 접수 직후 잠깐은 전부 null일 수 있음
    val snapshotTemperature: Double? = null,
    val snapshotHumidity: Double? = null,
    val snapshotIlluminance: Int? = null,
    val snapshotPeopleCount: Int? = null,
    val recommendedTemperatureBefore: Double? = null,
    val recommendedTemperatureAfter: Double? = null,
    // IoT 제어 대상이 아닌 피드백에 대한 AI 조언 - recommendedTemperature*와 상호 배타적.
    val aiAdvice: String? = null,
)
