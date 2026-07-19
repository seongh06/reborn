package com.reborn.core.network.remote

import com.reborn.core.network.datasource.DeviceDataSource
import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.device.PairingRequest
import com.reborn.core.network.model.request.device.RegisterDeviceRequest
import com.reborn.core.network.model.response.device.DeviceListResponse
import com.reborn.core.network.model.response.device.PairingCodeResponse
import com.reborn.core.network.model.response.device.PairingResponse
import com.reborn.core.network.model.response.device.RegisterDeviceResponse
import com.reborn.core.network.util.asApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody

// "auth" 클라이언트(#121) 사용 - generatePairingCode의 Authorization 헤더는 Auth 플러그인이 자동으로
// 붙인다. pairDevice는 애초에 인증 대상이 아니라 헤더를 붙이지 않는다(#113).
class DeviceDataSourceImpl(
    private val httpClient: HttpClient,
) : DeviceDataSource {

    override suspend fun generatePairingCode(placeId: Long): ApiResponse<PairingCodeResponse> = runCatching {
        httpClient.post("/api/device/pairing/code") {
            parameter("placeId", placeId)
        }
    }.asApiResponse()

    override suspend fun pairDevice(request: PairingRequest): ApiResponse<PairingResponse> = runCatching {
        httpClient.post("/api/device/pairing") {
            setBody(request)
        }
    }.asApiResponse()

    override suspend fun getList(placeId: Long): ApiResponse<DeviceListResponse> = runCatching {
        httpClient.get("/api/device") {
            parameter("placeId", placeId)
        }
    }.asApiResponse()

    override suspend fun registerDevice(request: RegisterDeviceRequest): ApiResponse<RegisterDeviceResponse> = runCatching {
        httpClient.post("/api/device") {
            setBody(request)
        }
    }.asApiResponse()
}
