package com.reborn.core.network.datasource

import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.smartthings.RegisterSmartThingsDeviceRequest
import com.reborn.core.network.model.response.device.RegisterDeviceResponse
import com.reborn.core.network.model.response.smartthings.AuthorizeResponse
import com.reborn.core.network.model.response.smartthings.SmartThingsDeviceListResponse

interface SmartThingsDataSource {
    // 동의 화면 authorizeUrl 발급(#130) - 앱이 이 URL을 외부 브라우저로 열어야 함
    suspend fun getAuthorizeUrl(placeId: Long): ApiResponse<AuthorizeResponse>

    // 연동된 SmartThings 계정의 전체 기기 목록(#132) - OAuth 동의가 끝난 뒤에만 성공
    suspend fun getDevices(placeId: Long): ApiResponse<SmartThingsDeviceListResponse>

    // 선택한 기기를 이 장소의 제어 대상으로 등록(#132) - device 테이블에 SMART_THINGS로 저장됨
    suspend fun registerDevice(request: RegisterSmartThingsDeviceRequest): ApiResponse<RegisterDeviceResponse>
}
