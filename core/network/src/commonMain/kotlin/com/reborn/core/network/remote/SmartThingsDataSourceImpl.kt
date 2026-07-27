package com.reborn.core.network.remote

import com.reborn.core.network.datasource.SmartThingsDataSource
import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.smartthings.RegisterSmartThingsDeviceRequest
import com.reborn.core.network.model.response.device.RegisterDeviceResponse
import com.reborn.core.network.model.response.smartthings.AuthorizeResponse
import com.reborn.core.network.model.response.smartthings.SmartThingsDeviceListResponse
import com.reborn.core.network.util.asApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class SmartThingsDataSourceImpl(
    private val httpClient: HttpClient,
) : SmartThingsDataSource {

    override suspend fun getAuthorizeUrl(placeId: Long): ApiResponse<AuthorizeResponse> = runCatching {
        httpClient.get("/api/smartthings/oauth/authorize") {
            parameter("placeId", placeId)
        }
    }.asApiResponse()

    override suspend fun getDevices(placeId: Long): ApiResponse<SmartThingsDeviceListResponse> = runCatching {
        httpClient.get("/api/smartthings/devices") {
            parameter("placeId", placeId)
        }
    }.asApiResponse()

    override suspend fun registerDevice(request: RegisterSmartThingsDeviceRequest): ApiResponse<RegisterDeviceResponse> = runCatching {
        httpClient.post("/api/smartthings/devices") {
            setBody(request)
        }
    }.asApiResponse()
}
