package com.reborn.core.model

data class Feedback(
    val feedbackId: Long,
    val deviceName: String?,
    val content: String,
    val status: String,
    // ISO-8601 로컬 날짜시각(타임존 없음, 서버 LocalDateTime 그대로) - 표시용 포맷팅은 UI 레이어 책임
    val createdAt: String,
)
