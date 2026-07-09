package com.reborn.core.network.remote

import com.reborn.core.network.datasource.DeviceDataSource
import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.response.device.PairingCodeResponse
import com.reborn.core.network.util.asApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders

class DeviceDataSourceImpl(
    private val httpClient: HttpClient,
) : DeviceDataSource {

    override suspend fun generatePairingCode(accessToken: String, placeId: Long): ApiResponse<PairingCodeResponse> = runCatching {
        httpClient.post("/api/device/pairing/code") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("placeId", placeId)
        }
    }.asApiResponse()
}
