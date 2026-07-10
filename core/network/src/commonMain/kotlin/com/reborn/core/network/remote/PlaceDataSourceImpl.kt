package com.reborn.core.network.remote

import com.reborn.core.network.datasource.PlaceDataSource
import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.place.AdminInviteRequest
import com.reborn.core.network.model.request.place.RegisterPlaceRequest
import com.reborn.core.network.model.response.place.AdminCodeResponse
import com.reborn.core.network.model.response.place.AdminInviteResponse
import com.reborn.core.network.model.response.place.PlaceDetailResponse
import com.reborn.core.network.model.response.place.PlaceListResponse
import com.reborn.core.network.model.response.place.PlaceResponse
import com.reborn.core.network.util.asApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody

// "auth" 클라이언트(#121) 사용 - Authorization 헤더는 Auth 플러그인이 자동으로 붙인다.
class PlaceDataSourceImpl(
    private val httpClient: HttpClient,
) : PlaceDataSource {

    override suspend fun register(request: RegisterPlaceRequest): ApiResponse<PlaceResponse> = runCatching {
        httpClient.post("/api/place") {
            setBody(request)
        }
    }.asApiResponse()

    override suspend fun generateAdminCode(placeId: Long): ApiResponse<AdminCodeResponse> = runCatching {
        httpClient.post("/api/place/admin/code") {
            parameter("placeId", placeId)
        }
    }.asApiResponse()

    override suspend fun redeemAdminCode(request: AdminInviteRequest): ApiResponse<AdminInviteResponse> = runCatching {
        httpClient.post("/api/place/admin") {
            setBody(request)
        }
    }.asApiResponse()

    override suspend fun getList(): ApiResponse<PlaceListResponse> = runCatching {
        httpClient.get("/api/place")
    }.asApiResponse()

    override suspend fun getDetail(placeId: Long): ApiResponse<PlaceDetailResponse> = runCatching {
        httpClient.get("/api/place/$placeId")
    }.asApiResponse()

    override suspend fun delete(placeId: Long): ApiResponse<Unit?> = runCatching {
        httpClient.delete("/api/place/$placeId")
    }.asApiResponse()
}
