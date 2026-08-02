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
    suspend fun leave(placeId: Long): Result<Unit>
    suspend fun transferOwner(placeId: Long, newOwnerUserId: Long): Result<Unit>

    // Home/Data/기기 등록 화면이 공유하는 "지금 선택된 장소"(#166) - 서버 데이터가 아니라 순수
    // 인메모리 UI 선택 상태라 별도 원격 조회 없이 동기로 읽고 쓴다. 앱 재시작 시 리셋된다.
    fun getSelectedPlaceId(): Long?
    fun selectPlace(placeId: Long)
}
