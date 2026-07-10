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
    val status: String,
    val createdAt: String,
)
