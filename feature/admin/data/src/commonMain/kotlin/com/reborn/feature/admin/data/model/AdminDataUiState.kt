package com.reborn.feature.admin.data.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AdminDataUiState {
    data object Loading : AdminDataUiState
    data class Data(
        val place: String = "Room01",
        val selectedCategory: Category = Category.TEMPERATURE,
        val chartLabels: List<String> = emptyList(),
        val chartValues: List<Float> = emptyList()
    ) : AdminDataUiState

    enum class Category(val label: String) {
        TEMPERATURE("온도"),
        HUMIDITY("습도"),
        ILLUMINANCE("조도"),
        PEOPLE_COUNT("재실 인원"),
        DISCOMFORT("불쾌지수")
    }
}

sealed interface AdminDataIntent {
    data object LoadInitial : AdminDataIntent
    data class ClickCategoryTab(val category: AdminDataUiState.Category) : AdminDataIntent
}
