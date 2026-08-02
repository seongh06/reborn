package com.reborn.server.domain.feedback.converter

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.feedback.Feedback
import com.reborn.server.domain.feedback.dto.FeedbackDto
import com.reborn.server.domain.place.Place
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

    fun toStatusUpdateResponse(entity: Feedback, controlSent: Boolean = false): FeedbackDto.StatusUpdateResponse =
        FeedbackDto.StatusUpdateResponse(feedbackId = entity.id, status = entity.status.name, controlSent = controlSent)

    // deviceId에 deviceKey를 그대로 노출하면 안 됨 - deviceKey는 Arduino/AI 스피커가 자체 인증에
    // 쓰는 비밀값(X-Device-Id 헤더, POST /api/metric/collect 등)이라 비로그인 공개 API로 유출되면
    // 그 값으로 다른 기기 API를 흉내낼 수 있음. DB 내부 id를 대신 공개 식별자로 사용(CodeRabbit 리뷰)
    fun toContextResponse(place: Place, devices: List<Device>, hasControllableDevice: Boolean): FeedbackDto.ContextResponse =
        FeedbackDto.ContextResponse(
            placeName = place.name,
            devices = devices.map {
                FeedbackDto.DeviceOption(deviceId = it.id.toString(), name = it.name ?: it.deviceType.name)
            },
            hasControllableDevice = hasControllableDevice,
        )

    private fun toFeedbackItem(entity: Feedback): FeedbackDto.FeedbackItem =
        FeedbackDto.FeedbackItem(
            feedbackId = entity.id,
            deviceId = entity.device?.deviceKey ?: "",
            deviceName = entity.device?.name,
            content = entity.content,
            source = entity.source.name,
            status = entity.status.name,
            isRead = entity.isRead,
            createdAt = requireNotNull(entity.createdAt),
            snapshotTemperature = entity.snapshotTemperature,
            snapshotHumidity = entity.snapshotHumidity,
            snapshotIlluminance = entity.snapshotIlluminance,
            snapshotPeopleCount = entity.snapshotPeopleCount,
            recommendedTemperatureBefore = entity.recommendedTemperatureBefore,
            recommendedTemperatureAfter = entity.recommendedTemperatureAfter,
            aiAdvice = entity.aiAdvice,
        )
}
