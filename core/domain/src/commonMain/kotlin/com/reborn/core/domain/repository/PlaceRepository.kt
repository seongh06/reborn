package com.reborn.core.domain.repository

import com.reborn.core.model.AdminInviteCode
import com.reborn.core.model.Place
import com.reborn.core.model.PlaceAdmin
import com.reborn.core.model.PlaceDetail
import com.reborn.core.model.PlaceMembership
import com.reborn.core.model.PlaceWifi

interface PlaceRepository {
    suspend fun register(name: String, type: String): Result<Place>
    suspend fun generateAdminCode(placeId: Long): Result<AdminInviteCode>
    suspend fun redeemAdminCode(adminCode: String): Result<PlaceMembership>
    suspend fun getList(): Result<List<Place>>
    suspend fun getDetail(placeId: Long): Result<PlaceDetail>
    suspend fun getAdmins(placeId: Long): Result<List<PlaceAdmin>>
    suspend fun getWifi(placeId: Long): Result<PlaceWifi>
    suspend fun updateWifi(placeId: Long, ssid: String, password: String): Result<PlaceWifi>
    suspend fun delete(placeId: Long): Result<Unit>
}
