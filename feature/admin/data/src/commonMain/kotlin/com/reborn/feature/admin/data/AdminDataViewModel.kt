package com.reborn.feature.admin.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.data.mapper.toSensorPoints
import com.reborn.core.network.model.SensorHistoryResponse
import com.reborn.core.network.service.SensorHistoryApi
import com.reborn.feature.admin.data.model.AdminDataIntent
import com.reborn.feature.admin.data.model.AdminDataUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private const val MOCK_DEVICE_ID = 1

private val DAYS_IN_MONTH = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

private data class MockDate(val year: Int, val month: Int, val day: Int)

// TODO: 실제 날짜/시간대 연동 전까지 "오늘"을 고정값으로 사용하는 목업. 실제 연동 시 kotlinx-datetime의 현재 시각으로 대체 예정
private val today = MockDate(2026, 7, 4)

private fun Int.pad2(): String = toString().padStart(2, '0')

private fun MockDate.toDateKey(): String = "$year${month.pad2()}${day.pad2()}"

private fun MockDate.minusDays(days: Int): MockDate {
    var y = year
    var m = month
    var d = day - days
    while (d <= 0) {
        m -= 1
        if (m == 0) {
            m = 12
            y -= 1
        }
        d += DAYS_IN_MONTH[m - 1]
    }
    return MockDate(y, m, d)
}

private fun MockDate.minusMonths(months: Int): MockDate {
    var total = month - 1 - months
    var y = year
    while (total < 0) {
        total += 12
        y -= 1
    }
    return MockDate(y, total % 12 + 1, day)
}

private data class ChartShape(val base: Float, val amplitudeRatio: Float, val frequency: Float, val phase: Float)

// 카테고리마다 진폭·주기·위상을 다르게 줘서 탭 전환 시 그래프 모양이 실제로 달라지도록 함
private fun chartShapeFor(category: AdminDataUiState.Category): ChartShape = when (category) {
    AdminDataUiState.Category.TEMPERATURE -> ChartShape(24f, 0.15f, 1f, -PI.toFloat() / 2f)
    AdminDataUiState.Category.HUMIDITY -> ChartShape(55f, 0.2f, 1f, PI.toFloat() / 2f)
    AdminDataUiState.Category.ILLUMINANCE -> ChartShape(300f, 0.6f, 1f, -PI.toFloat() / 2f)
    AdminDataUiState.Category.PEOPLE_COUNT -> ChartShape(3f, 0.8f, 1.5f, 0f)
    AdminDataUiState.Category.DISCOMFORT -> ChartShape(70f, 0.25f, 2f, 0f)
}

// TODO: 서버 /api/data/history 연동 확정 후 실제 Ktor 구현체로 교체하고 Koin으로 주입받도록 변경 예정.
// 최근 14일치를 "날짜별 1시간 단위 값" 형태로 생성 — 실제 API가 최적화된 형태(Map<날짜, 시간별 값>)로 내려주는 것을 가정한 목업
private class MockSensorHistoryApi : SensorHistoryApi {
    override suspend fun getSensorHistory(deviceId: Int, sensorType: String): SensorHistoryResponse {
        val category = AdminDataUiState.Category.entries.first { it.name == sensorType }
        val shape = chartShapeFor(category)
        val amplitude = shape.base * shape.amplitudeRatio

        val dailyData = (13 downTo 0).associate { daysAgo ->
            val date = today.minusDays(daysAgo)
            date.toDateKey() to (0..23).map { hour ->
                val angle = (2f * PI.toFloat() * shape.frequency * hour / 24f) + shape.phase
                (shape.base + amplitude * sin(angle)).toDouble()
            }
        }
        return SensorHistoryResponse(deviceId = deviceId, sensorType = sensorType, dailyData = dailyData)
    }
}

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

    private val sensorHistoryApi: SensorHistoryApi = MockSensorHistoryApi()

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
        val current = navigationManager.uiState.value as? AdminDataUiState.Data ?: return
        viewModelScope.launch {
            val chartValues = mockChartValues(category, current.selectedPeriod)
            navigationManager.updateCurrentState { state ->
                if (state is AdminDataUiState.Data) {
                    state.copy(
                        selectedCategory = category,
                        chartValues = chartValues,
                        analysisText = mockAnalysisText(category)
                    )
                } else state
            }
        }
    }

    private fun handlePeriodClick(period: AdminDataUiState.Period) {
        val current = navigationManager.uiState.value as? AdminDataUiState.Data ?: return
        viewModelScope.launch {
            val chartValues = mockChartValues(current.selectedCategory, period)
            navigationManager.updateCurrentState { state ->
                if (state is AdminDataUiState.Data) {
                    state.copy(
                        selectedPeriod = period,
                        chartLabels = chartLabelsFor(period),
                        chartValues = chartValues
                    )
                } else state
            }
        }
    }

    // Period는 "총 조회 범위"가 아니라 "점 사이의 간격"을 의미함 (1시간 = 점 하나가 1시간 간격, 일 = 점 하나가 하루 간격 ...)
    // 모든 기간이 실제 달력 개념(시각/날짜/월)으로 통일된 라벨을 쓰도록 함, 자연스러운 개수만큼만 표시(억지로 채우지 않음)
    private fun chartLabelsFor(period: AdminDataUiState.Period): List<String> {
        return when (period) {
            AdminDataUiState.Period.HOUR -> (0..23).map { hour -> "${hour.pad2()}:00" } // 1시간 간격 · 최근 24시간
            AdminDataUiState.Period.DAY -> (13 downTo 0).map { daysAgo ->               // 1일 간격 · 최근 14일
                val date = today.minusDays(daysAgo)
                "${date.month.pad2()}/${date.day.pad2()}"
            }
            AdminDataUiState.Period.WEEK -> (7 downTo 0).map { weeksAgo ->              // 1주 간격 · 최근 8주(달로 표시)
                "${today.minusDays(weeksAgo * 7).month}월"
            }
            AdminDataUiState.Period.MONTH -> (11 downTo 0).map { monthsAgo ->           // 1개월 간격 · 최근 12개월
                "${today.minusMonths(monthsAgo).month}월"
            }
            AdminDataUiState.Period.YEAR -> (4 downTo 0).map { yearsAgo -> "${today.year - yearsAgo}년" } // 1년 간격 · 최근 5년
        }
    }

    private suspend fun mockChartValues(category: AdminDataUiState.Category, period: AdminDataUiState.Period): List<Float> {
        return when (period) {
            AdminDataUiState.Period.HOUR -> hourlyValues(category)
            AdminDataUiState.Period.DAY -> dailyAverageValues(category)
            // 주/월/년 같은 장기 집계는 시간 단위 "일별 팩" 응답만으로 감당하기 어려워(연 단위면 시간당 값이 수만 개) 별도 목업 유지.
            // 실제 연동 시에는 전용 집계(주/월/년) API 응답을 받아 동일하게 매핑하면 됨
            AdminDataUiState.Period.WEEK,
            AdminDataUiState.Period.MONTH,
            AdminDataUiState.Period.YEAR -> longRangeValues(category, period)
        }
    }

    // TODO: 서버 sensorLogs 히스토리 조회 API 연동 전까지의 목업. 실제 연동 시 SensorHistoryApi의 Ktor 구현체로 대체 예정
    private suspend fun hourlyValues(category: AdminDataUiState.Category): List<Float> {
        val points = sensorHistoryApi.getSensorHistory(MOCK_DEVICE_ID, category.name).toSensorPoints()
        val todayKey = today.toDateKey()
        return points.filter { it.date == todayKey }
            .sortedBy { it.hour }
            .map { it.value.toFloat() }
    }

    private suspend fun dailyAverageValues(category: AdminDataUiState.Category): List<Float> {
        val points = sensorHistoryApi.getSensorHistory(MOCK_DEVICE_ID, category.name).toSensorPoints()
        return points.groupBy { it.date }
            .toSortedMap()
            .map { (_, dayPoints) -> dayPoints.map { it.value }.average().toFloat() }
    }

    private fun longRangeValues(category: AdminDataUiState.Category, period: AdminDataUiState.Period): List<Float> {
        val shape = chartShapeFor(category)
        val amplitude = shape.base * shape.amplitudeRatio
        val size = chartLabelsFor(period).size
        return List(size) { index ->
            val angle = (2f * PI.toFloat() * shape.frequency * index / size) + shape.phase
            shape.base + amplitude * sin(angle)
        }
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
