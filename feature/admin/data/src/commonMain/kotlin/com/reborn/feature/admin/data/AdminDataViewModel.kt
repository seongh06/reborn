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

private const val MOCK_DEVICE_ID = 1

// 목업 시간별 히스토리("어제"/"오늘")의 실제 일수. hourlyDayPatternsFor의 패턴 개수와 항상 같이 맞춰서 사용
private const val MOCK_HISTORY_DAY_COUNT = 2

private val DAYS_IN_MONTH = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

private data class MockDate(val year: Int, val month: Int, val day: Int)

// TODO: 실제 날짜/시간대 연동 전까지 "오늘"을 고정값으로 사용하는 목업. 실제 연동 시 kotlinx-datetime의 현재 시각으로 대체 예정
private val today = MockDate(2026, 7, 4)

// TODO: 실제 기기 등록/센서 수집 시작일 연동 전까지의 목업 (Phase 1 MVP 시작 시점인 06.01 기준)
private val dataCollectionStartDate = MockDate(2026, 6, 1)

private fun MockDate.toEpochDayApprox(): Int {
    val cumulativeDaysBeforeMonth = DAYS_IN_MONTH.take(month - 1).sum()
    return year * 365 + cumulativeDaysBeforeMonth + day
}

private fun elapsedDaysSinceDataCollectionStart(): Int =
    today.toEpochDayApprox() - dataCollectionStartDate.toEpochDayApprox()

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

// 카테고리별 하루치(0시~23시, 1시간 단위) 목업 값을 계산식이 아니라 실제 값처럼 보이는 숫자로 직접 나열.
// 날마다 완전히 똑같지 않도록 카테고리당 2개 패턴을 두고 번갈아 사용
private fun hourlyDayPatternsFor(category: AdminDataUiState.Category): List<List<Double>> = when (category) {
    AdminDataUiState.Category.TEMPERATURE -> listOf(
        listOf(18.0, 17.5, 17.0, 17.0, 17.5, 18.0, 19.0, 20.5, 22.0, 23.5, 24.5, 25.0, 25.5, 26.0, 25.5, 25.0, 24.0, 23.0, 22.0, 21.0, 20.0, 19.5, 19.0, 18.5),
        listOf(19.0, 18.5, 18.0, 18.5, 19.5, 21.0, 22.5, 24.0, 25.5, 26.5, 27.0, 27.5, 28.0, 27.5, 27.0, 26.0, 25.0, 23.5, 22.0, 21.0, 20.5, 20.0, 19.5, 19.0)
    )
    AdminDataUiState.Category.HUMIDITY -> listOf(
        listOf(68.0, 69.0, 70.0, 71.0, 70.0, 68.0, 65.0, 60.0, 55.0, 50.0, 47.0, 45.0, 44.0, 43.0, 45.0, 47.0, 50.0, 54.0, 58.0, 61.0, 64.0, 66.0, 67.0, 68.0),
        listOf(60.0, 61.0, 62.0, 63.0, 62.0, 60.0, 57.0, 53.0, 49.0, 45.0, 42.0, 40.0, 39.0, 39.0, 41.0, 43.0, 46.0, 50.0, 54.0, 57.0, 58.0, 59.0, 60.0, 60.0)
    )
    AdminDataUiState.Category.ILLUMINANCE -> listOf(
        listOf(0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 80.0, 200.0, 350.0, 500.0, 620.0, 700.0, 720.0, 700.0, 650.0, 560.0, 420.0, 280.0, 120.0, 40.0, 5.0, 0.0, 0.0, 0.0),
        listOf(0.0, 0.0, 0.0, 0.0, 10.0, 40.0, 120.0, 260.0, 400.0, 540.0, 650.0, 730.0, 750.0, 730.0, 680.0, 590.0, 450.0, 300.0, 140.0, 50.0, 10.0, 0.0, 0.0, 0.0)
    )
    AdminDataUiState.Category.PEOPLE_COUNT -> listOf(
        listOf(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 2.0, 3.0, 4.0, 4.0, 5.0, 5.0, 4.0, 4.0, 3.0, 3.0, 4.0, 5.0, 6.0, 4.0, 2.0, 1.0, 0.0),
        listOf(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 3.0, 3.0, 4.0, 4.0, 5.0, 5.0, 4.0, 3.0, 3.0, 4.0, 6.0, 7.0, 5.0, 3.0, 1.0, 1.0)
    )
    AdminDataUiState.Category.DISCOMFORT -> listOf(
        listOf(58.0, 57.0, 56.0, 56.0, 57.0, 59.0, 62.0, 66.0, 70.0, 74.0, 77.0, 79.0, 80.0, 81.0, 80.0, 78.0, 75.0, 71.0, 68.0, 65.0, 63.0, 61.0, 60.0, 59.0),
        listOf(60.0, 59.0, 58.0, 58.0, 59.0, 61.0, 64.0, 68.0, 72.0, 76.0, 79.0, 81.0, 82.0, 83.0, 82.0, 80.0, 77.0, 73.0, 70.0, 67.0, 65.0, 63.0, 62.0, 61.0)
    )
}

// 카테고리별 목업(주/월/년) — 자연스러운 값처럼 보이는 숫자를 그대로 나열 (계산식으로 만들지 않음)
private fun weeklyMockValues(category: AdminDataUiState.Category): List<Double> = when (category) { // 8개
    AdminDataUiState.Category.TEMPERATURE -> listOf(23.0, 24.0, 22.0, 25.0, 26.0, 24.0, 23.0, 25.0)
    AdminDataUiState.Category.HUMIDITY -> listOf(55.0, 58.0, 60.0, 52.0, 50.0, 57.0, 62.0, 54.0)
    AdminDataUiState.Category.ILLUMINANCE -> listOf(280.0, 300.0, 260.0, 320.0, 310.0, 290.0, 270.0, 305.0)
    AdminDataUiState.Category.PEOPLE_COUNT -> listOf(2.0, 3.0, 4.0, 3.0, 5.0, 4.0, 3.0, 2.0)
    AdminDataUiState.Category.DISCOMFORT -> listOf(68.0, 70.0, 72.0, 66.0, 64.0, 71.0, 74.0, 69.0)
}

private fun monthlyMockValues(category: AdminDataUiState.Category): List<Double> = when (category) { // 12개
    AdminDataUiState.Category.TEMPERATURE -> listOf(20.0, 21.0, 23.0, 25.0, 27.0, 29.0, 30.0, 29.0, 26.0, 23.0, 21.0, 19.0)
    AdminDataUiState.Category.HUMIDITY -> listOf(65.0, 60.0, 55.0, 50.0, 48.0, 52.0, 60.0, 68.0, 72.0, 66.0, 60.0, 58.0)
    AdminDataUiState.Category.ILLUMINANCE -> listOf(200.0, 240.0, 280.0, 320.0, 360.0, 380.0, 370.0, 340.0, 300.0, 260.0, 220.0, 190.0)
    AdminDataUiState.Category.PEOPLE_COUNT -> listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 6.0, 5.0, 4.0, 3.0, 2.0, 1.0)
    AdminDataUiState.Category.DISCOMFORT -> listOf(55.0, 58.0, 62.0, 68.0, 74.0, 80.0, 83.0, 81.0, 75.0, 68.0, 60.0, 56.0)
}

private fun yearlyMockValues(category: AdminDataUiState.Category): List<Double> = when (category) { // 5개
    AdminDataUiState.Category.TEMPERATURE -> listOf(22.0, 23.0, 24.0, 24.0, 25.0)
    AdminDataUiState.Category.HUMIDITY -> listOf(58.0, 60.0, 57.0, 59.0, 61.0)
    AdminDataUiState.Category.ILLUMINANCE -> listOf(290.0, 300.0, 310.0, 295.0, 305.0)
    AdminDataUiState.Category.PEOPLE_COUNT -> listOf(3.0, 4.0, 3.0, 4.0, 3.0)
    AdminDataUiState.Category.DISCOMFORT -> listOf(70.0, 72.0, 69.0, 71.0, 73.0)
}

// TODO: 서버 /api/data/history 연동 확정 후 실제 Ktor 구현체로 교체하고 Koin으로 주입받도록 변경 예정.
// 카테고리당 리터럴 패턴 2개 = 딱 2일치("어제", "오늘")만 생성 — 반복/순환 없이 그대로 사용
private class MockSensorHistoryApi : SensorHistoryApi {
    override suspend fun getSensorHistory(deviceId: Int, sensorType: String): SensorHistoryResponse {
        val category = AdminDataUiState.Category.entries.first { it.name == sensorType }
        val patterns = hourlyDayPatternsFor(category)

        val dailyData = patterns.indices.associate { index ->
            val daysAgo = patterns.size - 1 - index
            val date = today.minusDays(daysAgo)
            date.toDateKey() to patterns[index]
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
                    hasEnoughData = hasEnoughDataFor(period),
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
                        chartValues = chartValues,
                        hasEnoughData = hasEnoughDataFor(period)
                    )
                } else state
            }
        }
    }

    // 기기가 등록된 지 얼마 안 돼서 해당 기간 단위로 충분한 데이터가 쌓이지 않았으면 그래프 대신 안내 문구를 보여주기 위한 판단
    // (1시간은 오늘 하루치라 항상 표시, 일/주/월/년은 각각 최소 6개 단위만큼 쌓였을 때만 표시)
    // 일(DAY)은 실제 목업 히스토리 일수(MOCK_HISTORY_DAY_COUNT)로, 주/월/년은 데이터 수집 시작일로부터 경과한 시간으로 판단
    private fun hasEnoughDataFor(period: AdminDataUiState.Period): Boolean {
        val elapsedDays = elapsedDaysSinceDataCollectionStart()
        return when (period) {
            AdminDataUiState.Period.HOUR -> true
            AdminDataUiState.Period.DAY -> MOCK_HISTORY_DAY_COUNT >= 6
            AdminDataUiState.Period.WEEK -> elapsedDays / 7 >= 6
            AdminDataUiState.Period.MONTH -> elapsedDays / 30 >= 6
            AdminDataUiState.Period.YEAR -> elapsedDays / 365 >= 6
        }
    }

    // Period는 "총 조회 범위"가 아니라 "점 사이의 간격"을 의미함 (1시간 = 점 하나가 1시간 간격, 일 = 점 하나가 하루 간격 ...)
    // 모든 기간이 실제 달력 개념(시각/날짜/월)으로 통일된 라벨을 쓰도록 함, 자연스러운 개수만큼만 표시(억지로 채우지 않음)
    private fun chartLabelsFor(period: AdminDataUiState.Period): List<String> {
        return when (period) {
            // 1시간 간격 · 목업이 딱 2일치(어제/오늘)라 그 2일을 이어서 표시. 자정(0시)엔 "HH:00" 대신 그날 날짜(ex. "3일")로 표시해 날짜가 바뀌었음을 알림
            AdminDataUiState.Period.HOUR -> (MOCK_HISTORY_DAY_COUNT - 1 downTo 0).flatMap { daysAgo ->
                val date = today.minusDays(daysAgo)
                (0..23).map { hour -> if (hour == 0) "${date.day}일" else "${hour.pad2()}:00" }
            }
            // 1일 간격 · 목업 2일치(어제/오늘). 평소엔 날짜만(ex. "4일"), 월이 바뀌는 지점만 월로 표시(ex. "7월")
            AdminDataUiState.Period.DAY -> (MOCK_HISTORY_DAY_COUNT - 1 downTo 0).map { daysAgo ->
                val date = today.minusDays(daysAgo)
                if (date.day == 1) "${date.month}월" else "${date.day}일"
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
        if (!hasEnoughDataFor(period)) return emptyList()
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
    // 오늘 하루로 제한하지 않고 목업 전체 기간(14일치)을 이어서 반환 — 축소하면 여러 날짜가 쭉 이어져 보이도록 함
    private suspend fun hourlyValues(category: AdminDataUiState.Category): List<Float> {
        // toSensorPoints()가 이미 날짜 오름차순 → 하루 내 시간 오름차순으로 정렬해서 반환하므로 그대로 사용
        return sensorHistoryApi.getSensorHistory(MOCK_DEVICE_ID, category.name).toSensorPoints()
            .map { point -> point.value.toFloat() }
    }

    private suspend fun dailyAverageValues(category: AdminDataUiState.Category): List<Float> {
        val points = sensorHistoryApi.getSensorHistory(MOCK_DEVICE_ID, category.name).toSensorPoints()
        return points.groupBy { it.date }
            .toList()
            .sortedBy { (date, _) -> date }
            .map { (_, dayPoints) -> dayPoints.map { point -> point.value }.average().toFloat() }
    }

    // 주/월/년 목업도 계산식이 아니라 리터럴 숫자 배열을 그대로 사용
    private fun longRangeValues(category: AdminDataUiState.Category, period: AdminDataUiState.Period): List<Float> {
        val values = when (period) {
            AdminDataUiState.Period.WEEK -> weeklyMockValues(category)
            AdminDataUiState.Period.MONTH -> monthlyMockValues(category)
            AdminDataUiState.Period.YEAR -> yearlyMockValues(category)
            else -> emptyList()
        }
        return values.map { it.toFloat() }
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
