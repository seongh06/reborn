package com.reborn.core.data.mapper

import com.reborn.core.model.Feedback
import com.reborn.core.model.FeedbackCount
import com.reborn.core.network.model.response.feedback.FeedbackCountResponse
import com.reborn.core.network.model.response.feedback.FeedbackItemResponse

fun FeedbackItemResponse.toFeedback(): Feedback =
    Feedback(
        feedbackId = feedbackId,
        deviceId = deviceId,
        deviceName = deviceName,
        content = content,
        status = status,
        createdAt = createdAt,
    )

fun FeedbackCountResponse.toFeedbackCount(): FeedbackCount =
    FeedbackCount(total = total, pending = pending, approved = approved, rejected = rejected)
