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
