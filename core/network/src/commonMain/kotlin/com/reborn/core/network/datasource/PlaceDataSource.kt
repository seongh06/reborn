package com.reborn.core.network.datasource

import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.place.AdminInviteRequest
import com.reborn.core.network.model.request.place.RegisterPlaceRequest
import com.reborn.core.network.model.response.place.AdminCodeResponse
import com.reborn.core.network.model.response.place.AdminInviteResponse
import com.reborn.core.network.model.response.place.PlaceDetailResponse
import com.reborn.core.network.model.response.place.PlaceListResponse
import com.reborn.core.network.model.response.place.PlaceResponse

interface PlaceDataSource {
    suspend fun register(request: RegisterPlaceRequest): ApiResponse<PlaceResponse>
    suspend fun generateAdminCode(placeId: Long): ApiResponse<AdminCodeResponse>
    suspend fun redeemAdminCode(request: AdminInviteRequest): ApiResponse<AdminInviteResponse>
    suspend fun getList(): ApiResponse<PlaceListResponse>
    suspend fun getDetail(placeId: Long): ApiResponse<PlaceDetailResponse>
    suspend fun delete(placeId: Long): ApiResponse<Unit?>
}
