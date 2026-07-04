package com.reborn.feature.admin.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.feature.admin.data.model.AdminDataIntent
import com.reborn.feature.admin.data.model.AdminDataUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class AdminDataEvent {
    data object Exit : AdminDataEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminDataEvent()
}

class AdminDataViewModel : ViewModel() {
    private val navigationManager = NavigationManager<AdminDataUiState, AdminDataEvent>(
        initialState = AdminDataUiState.Loading,
        exitEvent = AdminDataEvent.Exit,
        scope = viewModelScope
    )

    val uiState = navigationManager.uiState
    val event = navigationManager.event

    fun onIntent(intent: AdminDataIntent) {
        when (intent) {
            is AdminDataIntent.LoadInitial -> checkInitialState()
            is AdminDataIntent.ClickCategoryTab -> handleCategoryClick(intent.category)
        }
    }

    private fun checkInitialState() {
        navigationManager.clearAndReset(AdminDataUiState.Loading)
        viewModelScope.launch {
            delay(1500)
            val category = AdminDataUiState.Category.TEMPERATURE
            navigationManager.clearAndReset(
                AdminDataUiState.Data(
                    selectedCategory = category,
                    chartLabels = chartLabelsFor(category),
                    chartValues = mockChartValues(category)
                )
            )
        }
    }

    private fun handleCategoryClick(category: AdminDataUiState.Category) {
        navigationManager.updateCurrentState { state ->
            if (state is AdminDataUiState.Data) {
                state.copy(
                    selectedCategory = category,
                    chartLabels = chartLabelsFor(category),
                    chartValues = mockChartValues(category)
                )
            } else state
        }
    }

    // TODO: 서버 sensorLogs 히스토리 조회 API 연동 전까지의 목업 데이터. 실제 연동 시 UseCase로 대체 예정
    private fun chartLabelsFor(category: AdminDataUiState.Category): List<String> {
        return listOf("0시", "4시", "8시", "12시", "16시", "20시")
    }

    private fun mockChartValues(category: AdminDataUiState.Category): List<Float> {
        val base = when (category) {
            AdminDataUiState.Category.TEMPERATURE -> 24f
            AdminDataUiState.Category.HUMIDITY -> 55f
            AdminDataUiState.Category.ILLUMINANCE -> 300f
            AdminDataUiState.Category.PEOPLE_COUNT -> 3f
            AdminDataUiState.Category.DISCOMFORT -> 70f
        }
        return List(6) { index -> base + (index % 3 - 1) * (base * 0.05f) }
    }
}
