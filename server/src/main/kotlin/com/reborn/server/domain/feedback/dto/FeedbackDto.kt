package com.reborn.server.domain.feedback.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

class FeedbackDto {

    data class SubmitRequest(
        @field:NotBlank val qrCode: String? = null,
        @field:NotBlank val deviceId: String? = null,
        @field:NotBlank val content: String? = null,
        @field:NotBlank val sessionToken: String? = null,
    )

    data class SubmitResponse(
        val feedbackId: Long,
        val status: String,
        val createdAt: LocalDateTime,
    )

    data class ListResponse(
        val totalCount: Long,
        val feedbacks: List<FeedbackItem>,
    )

    data class FeedbackItem(
        val feedbackId: Long,
        val deviceId: String,
        val deviceName: String?,
        val content: String,
        val status: String,
        val createdAt: LocalDateTime,
    )

    data class CountResponse(
        val total: Long,
        val pending: Long,
        val approved: Long,
        val rejected: Long,
    )

    data class StatusUpdateRequest(
        @field:NotBlank val status: String? = null,
    )

    data class StatusUpdateResponse(
        val feedbackId: Long,
        val status: String,
    )
}
