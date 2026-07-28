package com.reborn.core.network.datasource

import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.device.ControlDeviceRequest
import com.reborn.core.network.model.request.device.PairingRequest
import com.reborn.core.network.model.request.device.RegisterDeviceRequest
import com.reborn.core.network.model.response.device.ControlDeviceResponse
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

    // SmartThings로 등록된 기기(SMART_THINGS 타입)에 제어 명령 전송(#132/#134). deviceId는 device.deviceKey
    // (= SmartThings 기기 ID) 문자열 - DB 내부 id 아님, getList가 내려주는 DeviceItem.deviceId와 동일한 값.
    suspend fun controlDevice(deviceId: String, request: ControlDeviceRequest): ApiResponse<ControlDeviceResponse>

    // 기기 등록 해제(#189) - deviceId는 controlDevice와 동일하게 deviceKey
    suspend fun deleteDevice(deviceId: String): ApiResponse<Unit?>
}
