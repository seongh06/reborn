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
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders

class PlaceDataSourceImpl(
    private val httpClient: HttpClient,
) : PlaceDataSource {

    override suspend fun register(
        accessToken: String,
        request: RegisterPlaceRequest,
    ): ApiResponse<PlaceResponse> = runCatching {
        httpClient.post("/api/place") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(request)
        }
    }.asApiResponse()

    override suspend fun generateAdminCode(accessToken: String, placeId: Long): ApiResponse<AdminCodeResponse> = runCatching {
        httpClient.post("/api/place/admin/code") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("placeId", placeId)
        }
    }.asApiResponse()

    override suspend fun redeemAdminCode(
        accessToken: String,
        request: AdminInviteRequest,
    ): ApiResponse<AdminInviteResponse> = runCatching {
        httpClient.post("/api/place/admin") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(request)
        }
    }.asApiResponse()

    override suspend fun getList(accessToken: String): ApiResponse<PlaceListResponse> = runCatching {
        httpClient.get("/api/place") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }.asApiResponse()

    override suspend fun getDetail(accessToken: String, placeId: Long): ApiResponse<PlaceDetailResponse> = runCatching {
        httpClient.get("/api/place/$placeId") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }.asApiResponse()

    override suspend fun delete(accessToken: String, placeId: Long): ApiResponse<Unit?> = runCatching {
        httpClient.delete("/api/place/$placeId") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }.asApiResponse()
}
