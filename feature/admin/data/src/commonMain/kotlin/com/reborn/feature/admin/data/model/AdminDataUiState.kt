package com.reborn.feature.admin.data.model

import androidx.compose.runtime.Immutable
import com.reborn.core.ui.component.RoomOption

// AI 분석은 Gemini 호출 비용이 들어서(사용자 요청) 자동으로 부르지 않고, 데이터가 이 개수
// 이상 쌓였을 때만 "탭해서 보기" 형태로 노출 - 실제로 탭했을 때만 AI를 호출한다.
const val MIN_ANALYSIS_DATA_COUNT = 10

@Immutable
sealed interface AdminDataUiState {
    data object Loading : AdminDataUiState
    data class Data(
        val place: String = "Room01",
        val selectedCategory: Category = Category.TEMPERATURE,
        val selectedPeriod: Period = Period.HOUR,
        val chartLabels: List<String> = emptyList(),
        val chartValues: List<Float> = emptyList(),
        val hasEnoughData: Boolean = true,
        // null = 아직 AI 분석을 요청 전(블러+탭 대기 상태) - 사용자가 직접 탭해야 Gemini를
        // 호출한다(토큰 절약, 사용자 요청). 실제 AI 응답이 오면 이 필드가 채워진다.
        val analysisText: String? = null,
        val isAnalysisLoading: Boolean = false,
        // 조도/재실 인원은 공기계(AEROMETER)가 있어야 수집되는 값이라, 공기계가 연결 안 된
        // 장소에서는 해당 탭 자체를 노출하지 않는다(#236).
        val availableCategories: List<Category> = Category.entries,
        // 룸 전환(#166) - 선택 가능한 룸 목록과 현재 선택된 룸
        val rooms: List<RoomOption> = emptyList(),
        val selectedRoomId: Long? = null,
    ) : AdminDataUiState

    enum class Category(val label: String) {
        TEMPERATURE("온도"),
        HUMIDITY("습도"),
        ILLUMINANCE("조도"),
        PEOPLE_COUNT("재실 인원"),
        DISCOMFORT("불쾌지수")
    }

    enum class Period(val label: String) {
        HOUR("1시간"),
        DAY("일"),
        WEEK("주"),
        MONTH("월"),
        YEAR("년")
    }
}

sealed interface AdminDataIntent {
    data object LoadInitial : AdminDataIntent
    data class ClickCategoryTab(val category: AdminDataUiState.Category) : AdminDataIntent
    data class ClickPeriod(val period: AdminDataUiState.Period) : AdminDataIntent
    data object ClickExport : AdminDataIntent
    data object ClickRevealAnalysis : AdminDataIntent
    data class SelectPlace(val placeId: Long) : AdminDataIntent
}
