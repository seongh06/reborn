package com.reborn.feature.admin.data.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AdminDataUiState {
    data object Loading : AdminDataUiState
    data class Data(
        val place: String = "Room01",
        val selectedCategory: Category = Category.TEMPERATURE,
        val selectedPeriod: Period = Period.DAY,
        val chartLabels: List<String> = emptyList(),
        val chartValues: List<Float> = emptyList(),
        val hasEnoughData: Boolean = true,
        val analysisText: String = "",
        // 조도/재실 인원은 공기계(AEROMETER)가 있어야 수집되는 값이라, 공기계가 연결 안 된
        // 장소에서는 해당 탭 자체를 노출하지 않는다(#236).
        val availableCategories: List<Category> = Category.entries,
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
}
