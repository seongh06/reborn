package com.reborn.core.data.mapper

import com.reborn.core.model.Feedback
import com.reborn.core.network.model.response.feedback.FeedbackItemResponse

fun FeedbackItemResponse.toFeedback(): Feedback =
    Feedback(
        feedbackId = feedbackId,
        deviceName = deviceName,
        content = content,
        status = status,
        createdAt = createdAt,
    )
