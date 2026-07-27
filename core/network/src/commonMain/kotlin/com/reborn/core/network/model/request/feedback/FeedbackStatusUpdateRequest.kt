package com.reborn.core.network.model.request.feedback

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackStatusUpdateRequest(
    val status: String,
)
