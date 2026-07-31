package com.reborn.server.domain.feedback.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

class FeedbackDto {

    data class SubmitRequest(
        @field:NotBlank val qrCode: String? = null,
        // 이 장소에 기기가 없거나 사용자가 특정 기기를 지목하지 않으면 null로 온다 -
        // feedback.html은 selectedDeviceId가 null이어도 제출 버튼을 막지 않는다.
        val deviceId: String? = null,
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
        val source: String,
        val status: String,
        val createdAt: LocalDateTime,
        // "AI 맞춤 피드백" - 제출 직후 비동기로 채워지므로 접수 직후 잠깐은 전부 null일 수 있음
        val snapshotTemperature: Double? = null,
        val snapshotHumidity: Double? = null,
        val snapshotIlluminance: Int? = null,
        val snapshotPeopleCount: Int? = null,
        val recommendedTemperatureBefore: Double? = null,
        val recommendedTemperatureAfter: Double? = null,
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
        // 승인 시 이 피드백의 AI 추천 온도를 장소의 SmartThings 기기로 실제로 전송했는지 여부 -
        // 거절이거나, 추천값/SmartThings 기기가 없으면 false(정상 상태, 오류 아님)
        val controlSent: Boolean = false,
    )

    // QR 웹페이지(#163) 진입 시 장소명 + 제출 대상 기기 목록을 미리 조회하기 위한 응답
    data class ContextResponse(
        val placeName: String,
        val devices: List<DeviceOption>,
    )

    data class DeviceOption(
        val deviceId: String,
        val name: String,
    )
}
