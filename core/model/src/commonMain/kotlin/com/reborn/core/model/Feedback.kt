package com.reborn.core.model

data class Feedback(
    val feedbackId: Long,
    val deviceId: String,
    val deviceName: String?,
    val content: String,
    val status: String,
    val createdAt: String,
)

data class FeedbackCount(
    val total: Long,
    val pending: Long,
    val approved: Long,
    val rejected: Long,
)
