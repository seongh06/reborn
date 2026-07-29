package com.reborn.server.domain.place.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

class PlaceDto {

    data class RegisterRequest(
        // place.name 컬럼이 VARCHAR(255) - 넘으면 DB 저장 단계에서 502로 죽던 문제(#149),
        // 클라이언트 입력 제한(RebornTextField maxLength)에 더해 서버도 방어적으로 검증
        @field:NotBlank @field:Size(max = 255) val name: String? = null,
        @field:NotBlank val type: String? = null,
    )

    data class RegisterResponse(
        val placeId: Long,
        val name: String,
        val type: String,
        val createdAt: LocalDateTime,
    )

    data class AdminCodeResponse(
        val adminCode: String,
        val expiresAt: LocalDateTime,
    )

    data class AdminInviteRequest(
        @field:NotBlank val adminCode: String? = null,
    )

    data class AdminInviteResponse(
        val placeId: Long,
        val placeName: String,
        val accessLevel: String,
    )

    data class PlaceItem(
        val placeId: Long,
        val name: String,
        val type: String,
        val accessLevel: String,
        val createdAt: LocalDateTime,
    )

    data class ListResponse(
        val places: List<PlaceItem>,
    )

    data class DetailResponse(
        val placeId: Long,
        val name: String,
        val type: String,
        val accessLevel: String,
        val deviceCount: Int,
        val adminCount: Int,
        val qrCode: String,
        val createdAt: LocalDateTime,
    )

    data class AdminItem(
        val userId: Long,
        val name: String,
        val profileImage: String?,
    )

    data class AdminListResponse(
        val admins: List<AdminItem>,
    )
}
