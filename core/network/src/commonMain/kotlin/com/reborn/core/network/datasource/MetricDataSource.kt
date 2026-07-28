package com.reborn.core.network.datasource

import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.response.metric.MetricAggregateResponse
import com.reborn.core.network.model.response.metric.MetricAnalysisResponse
import com.reborn.core.network.model.response.metric.MetricCurrentResponse
import com.reborn.core.network.model.response.metric.MetricExportResponse
import com.reborn.core.network.model.response.metric.MetricHistoryResponse

interface MetricDataSource {
    suspend fun getCurrent(deviceId: String): ApiResponse<MetricCurrentResponse>
    suspend fun getHistory(deviceId: String, size: Int): ApiResponse<MetricHistoryResponse>

    // 주/월/년 장기 집계(데이터 화면 #158) - period는 WEEK/MONTH/YEAR만 지원
    suspend fun getAggregate(deviceId: String, period: String): ApiResponse<MetricAggregateResponse>

    // 카테고리별 AI 분석 문구(#158) - Gemini가 최신 측정값을 바탕으로 생성
    suspend fun getAnalysis(deviceId: String, category: String): ApiResponse<MetricAnalysisResponse>

    // Google Sheets로 최근 측정 이력 내보내기 - 장소가 미연동이면 서버가 404로 응답
    suspend fun exportToSheets(deviceId: String): ApiResponse<MetricExportResponse>
}
