package com.reborn.core.model

data class Place(
    val placeId: Long,
    val name: String,
    val type: String,
    val accessLevel: String,
    // 방장 여부(#추가 API) - 장소 하드 삭제/방장 위임은 방장만 가능, 그 외 관리자는 나가기만 가능
    val isOwner: Boolean = false,
    val createdAt: String,
)

data class PlaceDetail(
    val placeId: Long,
    val name: String,
    val type: String,
    val accessLevel: String,
    val isOwner: Boolean = false,
    val deviceCount: Int,
    val adminCount: Int,
    val qrCode: String,
    // QR 피드백 웹페이지 전체 URL - feature 모듈이 core:network의 AppConfig를 직접 참조하지
    // 않도록(코딩 가이드) data 레이어에서 미리 조립해 내려준다.
    val qrUrl: String,
    val createdAt: String,
)

data class PlaceMembership(
    val placeId: Long,
    val placeName: String,
    val accessLevel: String,
)

data class AdminInviteCode(
    val code: String,
    val expiresAt: String,
)

data class PlaceAdmin(
    val userId: Long,
    val name: String,
    val profileImage: String?,
    val isOwner: Boolean = false,
)

data class PlaceWifi(
    val ssid: String?,
    val password: String?,
)
