package com.reborn.core.network.datasource

import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.device.PairingRequest
import com.reborn.core.network.model.request.device.RegisterDeviceRequest
import com.reborn.core.network.model.response.device.DeviceListResponse
import com.reborn.core.network.model.response.device.PairingCodeResponse
import com.reborn.core.network.model.response.device.PairingResponse
import com.reborn.core.network.model.response.device.RegisterDeviceResponse

interface DeviceDataSource {
    suspend fun generatePairingCode(placeId: Long): ApiResponse<PairingCodeResponse>

    // 공기계 앱은 별도 로그인을 하지 않아 accessToken 없이 호출 - 페어링 코드 자체가 유일한 인가 수단(#113)
    suspend fun pairDevice(request: PairingRequest): ApiResponse<PairingResponse>

    suspend fun getList(placeId: Long): ApiResponse<DeviceListResponse>

    // Arduino 기기 등록 - 펌웨어에 하드코딩한 deviceId를 관리자가 직접 입력해 서버에 매칭시킨다.
    suspend fun registerDevice(request: RegisterDeviceRequest): ApiResponse<RegisterDeviceResponse>
}
