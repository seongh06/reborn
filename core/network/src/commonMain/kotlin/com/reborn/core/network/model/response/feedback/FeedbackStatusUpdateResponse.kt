package com.reborn.core.network.model.response.feedback

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackStatusUpdateResponse(
    val feedbackId: Long,
    val status: String,
    val controlSent: Boolean = false,
)
