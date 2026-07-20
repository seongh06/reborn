package com.reborn.server.domain.feedback.converter

import com.reborn.server.domain.feedback.Feedback
import com.reborn.server.domain.feedback.dto.FeedbackDto
import org.springframework.data.domain.Page

object FeedbackConverter {

    fun toSubmitResponse(entity: Feedback): FeedbackDto.SubmitResponse =
        FeedbackDto.SubmitResponse(
            feedbackId = entity.id,
            status = entity.status.name,
            createdAt = requireNotNull(entity.createdAt),
        )

    fun toListResponse(page: Page<Feedback>): FeedbackDto.ListResponse =
        FeedbackDto.ListResponse(
            totalCount = page.totalElements,
            feedbacks = page.content.map(::toFeedbackItem),
        )

    fun toCountResponse(total: Long, pending: Long, approved: Long, rejected: Long): FeedbackDto.CountResponse =
        FeedbackDto.CountResponse(total = total, pending = pending, approved = approved, rejected = rejected)

    fun toStatusUpdateResponse(entity: Feedback): FeedbackDto.StatusUpdateResponse =
        FeedbackDto.StatusUpdateResponse(feedbackId = entity.id, status = entity.status.name)

    private fun toFeedbackItem(entity: Feedback): FeedbackDto.FeedbackItem =
        FeedbackDto.FeedbackItem(
            feedbackId = entity.id,
            deviceId = entity.device?.deviceKey ?: "",
            deviceName = entity.device?.name,
            content = entity.content,
            source = entity.source.name,
            status = entity.status.name,
            createdAt = requireNotNull(entity.createdAt),
        )
}
