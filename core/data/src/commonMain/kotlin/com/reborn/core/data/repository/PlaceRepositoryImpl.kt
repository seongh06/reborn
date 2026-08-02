package com.reborn.core.data.repository

import com.reborn.core.data.mapper.toAdminInviteCode
import com.reborn.core.data.mapper.toPlace
import com.reborn.core.data.mapper.toPlaceAdmin
import com.reborn.core.data.mapper.toPlaceDetail
import com.reborn.core.data.mapper.toPlaceMembership
import com.reborn.core.data.mapper.toPlaceWifi
import com.reborn.core.data.mapper.toResult
import com.reborn.core.domain.repository.PlaceRepository
import com.reborn.core.model.AdminInviteCode
import com.reborn.core.model.Place
import com.reborn.core.model.PlaceAdmin
import com.reborn.core.model.PlaceDetail
import com.reborn.core.model.PlaceMembership
import com.reborn.core.model.PlaceWifi
import com.reborn.core.network.datasource.PlaceDataSource
import com.reborn.core.network.model.request.place.AdminInviteRequest
import com.reborn.core.network.model.request.place.RegisterPlaceRequest
import com.reborn.core.network.model.request.place.TransferOwnerRequest
import com.reborn.core.network.model.request.place.UpdatePlaceWifiRequest

class PlaceRepositoryImpl(
    private val remote: PlaceDataSource,
) : PlaceRepository {

    // "지금 선택된 장소"(#166) - 서버 데이터가 아닌 순수 인메모리 UI 상태라 이 Repository가 Koin
    // single로 등록돼있는 것에 기대 필드로 직접 들고 있는다(멀티플랫폼에서 별도 DataStore 없이
    // 세션 동안만 유지 - 앱 재시작 시 리셋되는데 기존에도 항상 첫 번째 장소만 봤으니 회귀 아님).
    private var selectedPlaceId: Long? = null

    override fun getSelectedPlaceId(): Long? = selectedPlaceId

    override fun selectPlace(placeId: Long) {
        selectedPlaceId = placeId
    }

    override suspend fun register(name: String, type: String): Result<Place> =
        remote.register(RegisterPlaceRequest(name, type))
            // 장소 등록은 항상 등록한 사람을 ADMIN이자 방장으로 만든다(서버 PlaceService.register()).
            .toResult { it.toPlace(isOwner = true) }

    override suspend fun generateAdminCode(placeId: Long): Result<AdminInviteCode> =
        remote.generateAdminCode(placeId)
            .toResult { it.toAdminInviteCode() }

    override suspend fun redeemAdminCode(adminCode: String): Result<PlaceMembership> =
        remote.redeemAdminCode(AdminInviteRequest(adminCode))
            .toResult { it.toPlaceMembership() }

    override suspend fun getList(): Result<List<Place>> =
        remote.getList()
            .toResult { response -> response.places.map { it.toPlace() } }

    override suspend fun getDetail(placeId: Long): Result<PlaceDetail> =
        remote.getDetail(placeId)
            .toResult { it.toPlaceDetail() }

    override suspend fun getAdmins(placeId: Long): Result<List<PlaceAdmin>> =
        remote.getAdmins(placeId)
            .toResult { response -> response.admins.map { it.toPlaceAdmin() } }

    override suspend fun getWifi(placeId: Long): Result<PlaceWifi> =
        remote.getWifi(placeId)
            .toResult { it.toPlaceWifi() }

    override suspend fun updateWifi(placeId: Long, ssid: String, password: String): Result<PlaceWifi> =
        remote.updateWifi(placeId, UpdatePlaceWifiRequest(ssid, password))
            .toResult { it.toPlaceWifi() }

    override suspend fun delete(placeId: Long): Result<Unit> =
        remote.delete(placeId)
            .toResult { }

    override suspend fun leave(placeId: Long): Result<Unit> =
        remote.leave(placeId)
            .toResult { }

    override suspend fun transferOwner(placeId: Long, newOwnerUserId: Long): Result<Unit> =
        remote.transferOwner(placeId, TransferOwnerRequest(newOwnerUserId))
            .toResult { }
}
