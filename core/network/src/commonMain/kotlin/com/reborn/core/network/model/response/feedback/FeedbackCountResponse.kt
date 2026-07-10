package com.reborn.core.network.model.response.feedback

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackCountResponse(
    val total: Long,
    val pending: Long,
    val approved: Long,
    val rejected: Long,
)
