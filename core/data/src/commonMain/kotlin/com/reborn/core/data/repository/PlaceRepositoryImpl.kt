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
import com.reborn.core.network.model.request.place.UpdatePlaceWifiRequest

class PlaceRepositoryImpl(
    private val remote: PlaceDataSource,
) : PlaceRepository {

    override suspend fun register(name: String, type: String): Result<Place> =
        remote.register(RegisterPlaceRequest(name, type))
            .toResult { it.toPlace() }

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
}
