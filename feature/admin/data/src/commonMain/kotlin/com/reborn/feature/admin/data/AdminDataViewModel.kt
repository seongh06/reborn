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
    data class ShowSnackbar(val message: String) : AdminDataEvent()
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
            is AdminDataIntent.ClickPeriod -> handlePeriodClick(intent.period)
            is AdminDataIntent.ClickExport -> exportToGoogleSheets()
        }
    }

    private fun checkInitialState() {
        navigationManager.clearAndReset(AdminDataUiState.Loading)
        viewModelScope.launch {
            delay(1500)
            val category = AdminDataUiState.Category.TEMPERATURE
            val period = AdminDataUiState.Period.DAY
            navigationManager.clearAndReset(
                AdminDataUiState.Data(
                    selectedCategory = category,
                    selectedPeriod = period,
                    chartLabels = chartLabelsFor(period),
                    chartValues = mockChartValues(category, period),
                    analysisText = mockAnalysisText(category)
                )
            )
        }
    }

    private fun handleCategoryClick(category: AdminDataUiState.Category) {
        navigationManager.updateCurrentState { state ->
            if (state is AdminDataUiState.Data) {
                state.copy(
                    selectedCategory = category,
                    chartValues = mockChartValues(category, state.selectedPeriod),
                    analysisText = mockAnalysisText(category)
                )
            } else state
        }
    }

    private fun handlePeriodClick(period: AdminDataUiState.Period) {
        navigationManager.updateCurrentState { state ->
            if (state is AdminDataUiState.Data) {
                state.copy(
                    selectedPeriod = period,
                    chartLabels = chartLabelsFor(period),
                    chartValues = mockChartValues(state.selectedCategory, period)
                )
            } else state
        }
    }

    // TODO: 서버 sensorLogs 히스토리 조회 API 연동 전까지의 목업 데이터. 실제 연동 시 UseCase로 대체 예정
    private fun chartLabelsFor(period: AdminDataUiState.Period): List<String> {
        return when (period) {
            AdminDataUiState.Period.HOUR -> listOf("0분", "10분", "20분", "30분", "40분", "50분")
            AdminDataUiState.Period.DAY -> listOf("0시", "4시", "8시", "12시", "16시", "20시")
            AdminDataUiState.Period.WEEK -> listOf("월", "화", "수", "목", "금", "토", "일")
            AdminDataUiState.Period.MONTH -> listOf("1주", "2주", "3주", "4주")
            AdminDataUiState.Period.YEAR -> listOf("1월", "4월", "7월", "10월")
        }
    }

    private fun mockChartValues(category: AdminDataUiState.Category, period: AdminDataUiState.Period): List<Float> {
        val base = when (category) {
            AdminDataUiState.Category.TEMPERATURE -> 24f
            AdminDataUiState.Category.HUMIDITY -> 55f
            AdminDataUiState.Category.ILLUMINANCE -> 300f
            AdminDataUiState.Category.PEOPLE_COUNT -> 3f
            AdminDataUiState.Category.DISCOMFORT -> 70f
        }
        val size = chartLabelsFor(period).size
        return List(size) { index -> base + (index % 3 - 1) * (base * 0.05f) }
    }

    // TODO: 서버 Google Sheets 내보내기 API 연동 전까지의 목업. 실제 연동 시 UseCase/Repository로 대체 예정
    private fun exportToGoogleSheets() {
        viewModelScope.launch {
            delay(500)
            navigationManager.emitEvent(AdminDataEvent.ShowSnackbar("Google Sheets로 내보냈습니다."))
        }
    }

    // TODO: AI 분석 응답 데이터 연동 전까지의 목업 텍스트. 실제 연동 시 서버 응답으로 대체 예정
    private fun mockAnalysisText(category: AdminDataUiState.Category): String {
        return when (category) {
            AdminDataUiState.Category.TEMPERATURE -> "현재 온도가 희망 온도보다 1도 정도 높습니다. 냉방을 가동하면 에너지 효율이 개선됩니다."
            AdminDataUiState.Category.HUMIDITY -> "실내 습도가 적정 범위를 벗어났습니다. 제습 모드를 권장합니다."
            AdminDataUiState.Category.ILLUMINANCE -> "조도가 낮은 시간대가 반복됩니다. 조명 자동 점등 설정을 검토해보세요."
            AdminDataUiState.Category.PEOPLE_COUNT -> "재실 인원이 몰리는 시간대에 에너지 소비가 집중되고 있습니다."
            AdminDataUiState.Category.DISCOMFORT -> "불쾌지수가 높은 구간이 감지됐습니다. 냉방 가동을 권장합니다."
        }
    }
}
