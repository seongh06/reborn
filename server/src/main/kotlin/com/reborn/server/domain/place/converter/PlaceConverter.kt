package com.reborn.server.domain.place.converter

import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.dto.PlaceDto

object PlaceConverter {

    fun toRegisterResponse(entity: Place): PlaceDto.RegisterResponse =
        PlaceDto.RegisterResponse(
            placeId = entity.id,
            name = entity.name,
            type = entity.type.name,
            createdAt = requireNotNull(entity.createdAt),
        )
}
