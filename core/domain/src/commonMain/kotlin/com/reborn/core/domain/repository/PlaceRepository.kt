package com.reborn.core.domain.repository

import com.reborn.core.model.AdminInviteCode
import com.reborn.core.model.Place
import com.reborn.core.model.PlaceDetail
import com.reborn.core.model.PlaceMembership

interface PlaceRepository {
    suspend fun register(name: String, type: String): Result<Place>
    suspend fun generateAdminCode(placeId: Long): Result<AdminInviteCode>
    suspend fun redeemAdminCode(adminCode: String): Result<PlaceMembership>
    suspend fun getList(): Result<List<Place>>
    suspend fun getDetail(placeId: Long): Result<PlaceDetail>
    suspend fun delete(placeId: Long): Result<Unit>
}
