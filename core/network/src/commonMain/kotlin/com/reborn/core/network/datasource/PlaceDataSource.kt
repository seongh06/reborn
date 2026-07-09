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
    suspend fun register(accessToken: String, request: RegisterPlaceRequest): ApiResponse<PlaceResponse>
    suspend fun generateAdminCode(accessToken: String, placeId: Long): ApiResponse<AdminCodeResponse>
    suspend fun redeemAdminCode(accessToken: String, request: AdminInviteRequest): ApiResponse<AdminInviteResponse>
    suspend fun getList(accessToken: String): ApiResponse<PlaceListResponse>
    suspend fun getDetail(accessToken: String, placeId: Long): ApiResponse<PlaceDetailResponse>
    suspend fun delete(accessToken: String, placeId: Long): ApiResponse<Unit?>
}
