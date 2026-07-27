package com.reborn.core.model

data class Place(
    val placeId: Long,
    val name: String,
    val type: String,
    val accessLevel: String,
    val createdAt: String,
)

data class PlaceDetail(
    val placeId: Long,
    val name: String,
    val type: String,
    val accessLevel: String,
    val deviceCount: Int,
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
