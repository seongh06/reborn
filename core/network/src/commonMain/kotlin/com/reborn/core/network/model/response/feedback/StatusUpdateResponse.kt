package com.reborn.core.network.model.response.feedback

import kotlinx.serialization.Serializable

@Serializable
data class StatusUpdateResponse(
    val feedbackId: Long,
    val status: String,
)
