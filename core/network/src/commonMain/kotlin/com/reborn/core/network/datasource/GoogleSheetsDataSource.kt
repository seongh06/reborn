package com.reborn.core.network.datasource

import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.response.googlesheets.GoogleSheetsAuthorizeResponse

interface GoogleSheetsDataSource {
    // 동의 화면 authorizeUrl 발급(데이터 화면 내보내기) - 앱이 이 URL을 외부 브라우저로 열어야 함
    suspend fun getAuthorizeUrl(placeId: Long): ApiResponse<GoogleSheetsAuthorizeResponse>
}
