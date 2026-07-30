package com.reborn.core.network.datasource

import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.place.AdminInviteRequest
import com.reborn.core.network.model.request.place.RegisterPlaceRequest
import com.reborn.core.network.model.request.place.TransferOwnerRequest
import com.reborn.core.network.model.request.place.UpdatePlaceWifiRequest
import com.reborn.core.network.model.response.place.AdminCodeResponse
import com.reborn.core.network.model.response.place.AdminInviteResponse
import com.reborn.core.network.model.response.place.PlaceAdminListResponse
import com.reborn.core.network.model.response.place.PlaceDetailResponse
import com.reborn.core.network.model.response.place.PlaceListResponse
import com.reborn.core.network.model.response.place.PlaceResponse
import com.reborn.core.network.model.response.place.PlaceWifiResponse
import com.reborn.core.network.model.response.place.TransferOwnerResponse

interface PlaceDataSource {
    suspend fun register(request: RegisterPlaceRequest): ApiResponse<PlaceResponse>
    suspend fun generateAdminCode(placeId: Long): ApiResponse<AdminCodeResponse>
    suspend fun redeemAdminCode(request: AdminInviteRequest): ApiResponse<AdminInviteResponse>
    suspend fun getList(): ApiResponse<PlaceListResponse>
    suspend fun getDetail(placeId: Long): ApiResponse<PlaceDetailResponse>
    suspend fun getAdmins(placeId: Long): ApiResponse<PlaceAdminListResponse>
    suspend fun getWifi(placeId: Long): ApiResponse<PlaceWifiResponse>
    suspend fun updateWifi(placeId: Long, request: UpdatePlaceWifiRequest): ApiResponse<PlaceWifiResponse>
    suspend fun delete(placeId: Long): ApiResponse<Unit?>
    suspend fun leave(placeId: Long): ApiResponse<Unit?>
    suspend fun transferOwner(placeId: Long, request: TransferOwnerRequest): ApiResponse<TransferOwnerResponse>
}
