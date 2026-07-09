package com.reborn.server.domain.place.converter

import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.UserPlaceMapping
import com.reborn.server.domain.place.dto.PlaceDto

object PlaceConverter {

    fun toRegisterResponse(entity: Place): PlaceDto.RegisterResponse =
        PlaceDto.RegisterResponse(
            placeId = entity.id,
            name = entity.name,
            type = entity.type.name,
            createdAt = requireNotNull(entity.createdAt),
        )

    fun toPlaceItem(mapping: UserPlaceMapping): PlaceDto.PlaceItem {
        val place = mapping.place
        return PlaceDto.PlaceItem(
            placeId = place.id,
            name = place.name,
            type = place.type.name,
            accessLevel = mapping.accessLevel.name,
            createdAt = requireNotNull(place.createdAt),
        )
    }

    fun toDetailResponse(entity: Place, accessLevel: AccessLevel, deviceCount: Int): PlaceDto.DetailResponse =
        PlaceDto.DetailResponse(
            placeId = entity.id,
            name = entity.name,
            type = entity.type.name,
            accessLevel = accessLevel.name,
            deviceCount = deviceCount,
            qrCode = entity.qrCode,
            createdAt = requireNotNull(entity.createdAt),
        )
}
