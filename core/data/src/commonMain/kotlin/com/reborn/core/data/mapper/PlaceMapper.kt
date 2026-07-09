package com.reborn.core.data.mapper

import com.reborn.core.model.AdminInviteCode
import com.reborn.core.model.Place
import com.reborn.core.model.PlaceDetail
import com.reborn.core.model.PlaceMembership
import com.reborn.core.network.model.response.place.AdminCodeResponse
import com.reborn.core.network.model.response.place.AdminInviteResponse
import com.reborn.core.network.model.response.place.PlaceDetailResponse
import com.reborn.core.network.model.response.place.PlaceItemResponse
import com.reborn.core.network.model.response.place.PlaceResponse

fun PlaceResponse.toPlace(accessLevel: String = "ADMIN"): Place =
    Place(placeId = placeId, name = name, type = type, accessLevel = accessLevel, createdAt = createdAt)

fun PlaceItemResponse.toPlace(): Place =
    Place(placeId = placeId, name = name, type = type, accessLevel = accessLevel, createdAt = createdAt)

fun PlaceDetailResponse.toPlaceDetail(): PlaceDetail =
    PlaceDetail(
        placeId = placeId,
        name = name,
        type = type,
        accessLevel = accessLevel,
        deviceCount = deviceCount,
        qrCode = qrCode,
        createdAt = createdAt,
    )

fun AdminCodeResponse.toAdminInviteCode(): AdminInviteCode =
    AdminInviteCode(code = adminCode, expiresAt = expiresAt)

fun AdminInviteResponse.toPlaceMembership(): PlaceMembership =
    PlaceMembership(placeId = placeId, placeName = placeName, accessLevel = accessLevel)
