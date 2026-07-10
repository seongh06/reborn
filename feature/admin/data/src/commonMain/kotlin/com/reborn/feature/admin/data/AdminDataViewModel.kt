package com.reborn.feature.admin.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.domain.usecase.GetDeviceListUseCase
import com.reborn.core.domain.usecase.GetMetricHistoryParams
import com.reborn.core.domain.usecase.GetMetricHistoryUseCase
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.core.model.MetricLog
import com.reborn.feature.admin.data.model.AdminDataIntent
import com.reborn.feature.admin.data.model.AdminDataUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val DAYS_IN_MONTH = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

private data class MockDate(val year: Int, val month: Int, val day: Int)

// TODO: 실제 날짜/시간대 연동 전까지 "오늘"을 고정값으로 사용하는 목업(WEEK/MONTH/YEAR 집계 전용).
// 실제 연동 시 kotlinx-datetime의 현재 시각으로 대체 예정
private val today = MockDate(2026, 7, 4)

// TODO: 실제 기기 등록/센서 수집 시작일 연동 전까지의 목업 (Phase 1 MVP 시작 시점인 06.01 기준)
private val dataCollectionStartDate = MockDate(2026, 6, 1)

private fun MockDate.toEpochDayApprox(): Int {
    val cumulativeDaysBeforeMonth = DAYS_IN_MONTH.take(month - 1).sum()
    return year * 365 + cumulativeDaysBeforeMonth + day
}

private fun elapsedDaysSinceDataCollectionStart(): Int =
    today.toEpochDayApprox() - dataCollectionStartDate.toEpochDayApprox()

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

// 카테고리별 목업(주/월/년) — 서버에 장기 집계 API가 없어 자연스러운 값처럼 보이는 숫자를 그대로 나열 (계산식 아님)
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

private data class LogBucketKey(val dateKey: String, val hour: Int) // dateKey: "yyyyMMdd"

// 서버 LocalDateTime은 "yyyy-MM-ddTHH:mm:ss" 형태의 ISO 문자열로 내려온다 (core:model의 createdAt: String 관례)
private fun MetricLog.bucketKey(): LogBucketKey {
    val dateKey = createdAt.substring(0, 10).replace("-", "")
    val hour = createdAt.substring(11, 13).toIntOrNull() ?: 0
    return LogBucketKey(dateKey, hour)
}

// 서버 히스토리 응답(MetricDto.HistoryItem)에는 discomfort 필드가 없어 실데이터로는 계산하지 않는다.
// TODO: 불쾌지수 공식이 서버와 동일하게 확정되면 temperature/humidity로 클라이언트 계산해 채우거나
// 서버 히스토리 응답에 discomfort 필드를 추가하는 방향으로 확장
private fun MetricLog.valueFor(category: AdminDataUiState.Category): Double? = when (category) {
    AdminDataUiState.Category.TEMPERATURE -> temperature
    AdminDataUiState.Category.HUMIDITY -> humidity
    AdminDataUiState.Category.ILLUMINANCE -> illuminance?.toDouble()
    AdminDataUiState.Category.PEOPLE_COUNT -> peopleCount?.toDouble()
    AdminDataUiState.Category.DISCOMFORT -> null
}

sealed class AdminDataEvent {
    data object Exit : AdminDataEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminDataEvent()
    data class ShowSnackbar(val message: String) : AdminDataEvent()
}

class AdminDataViewModel(
    private val getPlaceListUseCase: GetPlaceListUseCase,
    private val getDeviceListUseCase: GetDeviceListUseCase,
    private val getMetricHistoryUseCase: GetMetricHistoryUseCase,
) : ViewModel() {
    private val navigationManager = NavigationManager<AdminDataUiState, AdminDataEvent>(
        initialState = AdminDataUiState.Loading,
        exitEvent = AdminDataEvent.Exit,
        scope = viewModelScope
    )

    private var loadJob: Job? = null

    // 장소의 첫 ARDUINO 기기 히스토리 원본 로그. HOUR/DAY 구간 집계에 사용 (WEEK/MONTH/YEAR는 서버에
    // 집계 API가 없어 계속 목업 사용, #124)
    private var metricLogs: List<MetricLog> = emptyList()

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
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val category = AdminDataUiState.Category.TEMPERATURE
            val period = AdminDataUiState.Period.DAY
            try {
                metricLogs = loadMetricLogs()
                navigationManager.clearAndReset(
                    AdminDataUiState.Data(
                        selectedCategory = category,
                        selectedPeriod = period,
                        chartLabels = chartLabelsFor(period),
                        chartValues = chartValuesFor(category, period),
                        hasEnoughData = hasEnoughDataFor(period),
                        analysisText = mockAnalysisText(category)
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                navigationManager.emitEvent(AdminDataEvent.ShowErrorSnackbar(e))
            }
        }
    }

    // 관리자 앱이 여러 장소를 오가는 UI가 아직 없어 첫 장소의 첫 ARDUINO 기기로 임시 고정(#124, home과 동일 정책)
    private suspend fun loadMetricLogs(): List<MetricLog> {
        val place = getPlaceListUseCase().getOrNull()?.firstOrNull() ?: return emptyList()
        val device = getDeviceListUseCase(place.placeId).getOrNull()
            ?.firstOrNull { it.deviceType == "ARDUINO" }
            ?: return emptyList()
        return getMetricHistoryUseCase(GetMetricHistoryParams(device.deviceId, page = 0, size = 500))
            .getOrNull()?.logs.orEmpty()
    }

    private fun handleCategoryClick(category: AdminDataUiState.Category) {
        val current = navigationManager.uiState.value as? AdminDataUiState.Data ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val chartValues = chartValuesFor(category, current.selectedPeriod)
                navigationManager.updateCurrentState { state ->
                    if (state is AdminDataUiState.Data) {
                        state.copy(
                            selectedCategory = category,
                            chartValues = chartValues,
                            analysisText = mockAnalysisText(category)
                        )
                    } else state
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                navigationManager.emitEvent(AdminDataEvent.ShowErrorSnackbar(e))
            }
        }
    }

    private fun handlePeriodClick(period: AdminDataUiState.Period) {
        val current = navigationManager.uiState.value as? AdminDataUiState.Data ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val chartValues = chartValuesFor(current.selectedCategory, period)
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                navigationManager.emitEvent(AdminDataEvent.ShowErrorSnackbar(e))
            }
        }
    }

    // 기기가 등록된 지 얼마 안 돼서 해당 기간 단위로 충분한 데이터가 쌓이지 않았으면 그래프 대신 안내 문구를 보여줌.
    // HOUR/DAY는 실제 수집된 로그 유무로 판단, WEEK/MONTH/YEAR는 서버 집계 API가 없어 계속 목업 경과일 기준(TODO)
    private fun hasEnoughDataFor(period: AdminDataUiState.Period): Boolean {
        val elapsedDays = elapsedDaysSinceDataCollectionStart()
        return when (period) {
            AdminDataUiState.Period.HOUR -> metricLogs.isNotEmpty()
            AdminDataUiState.Period.DAY -> metricLogs.map { it.bucketKey().dateKey }.distinct().size >= 2
            AdminDataUiState.Period.WEEK -> elapsedDays / 7 >= 6
            AdminDataUiState.Period.MONTH -> elapsedDays / 30 >= 6
            AdminDataUiState.Period.YEAR -> elapsedDays / 365 >= 6
        }
    }

    private fun chartLabelsFor(period: AdminDataUiState.Period): List<String> {
        return when (period) {
            AdminDataUiState.Period.HOUR -> hourlyBucketKeys().let { keys ->
                var lastDate: String? = null
                keys.map { key ->
                    val isNewDate = key.dateKey != lastDate
                    lastDate = key.dateKey
                    if (isNewDate) "${key.dateKey.takeLast(2).toInt()}일" else "${key.hour.toString().padStart(2, '0')}:00"
                }
            }
            AdminDataUiState.Period.DAY -> dailyBucketDates().map { dateKey ->
                val month = dateKey.substring(4, 6).toInt()
                val day = dateKey.takeLast(2).toInt()
                if (day == 1) "${month}월" else "${day}일"
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

    private fun hourlyBucketKeys(): List<LogBucketKey> =
        metricLogs.map { it.bucketKey() }.distinct().sortedWith(compareBy({ it.dateKey }, { it.hour }))

    private fun dailyBucketDates(): List<String> =
        metricLogs.map { it.bucketKey().dateKey }.distinct().sorted()

    private fun chartValuesFor(category: AdminDataUiState.Category, period: AdminDataUiState.Period): List<Float> {
        if (!hasEnoughDataFor(period)) return emptyList()
        return when (period) {
            AdminDataUiState.Period.HOUR -> hourlyBucketKeys().map { key ->
                averageFor(category) { it.bucketKey() == key }
            }
            AdminDataUiState.Period.DAY -> dailyBucketDates().map { dateKey ->
                averageFor(category) { it.bucketKey().dateKey == dateKey }
            }
            // 주/월/년 같은 장기 집계는 서버에 전용 집계 API가 없어(연 단위면 원본 로그가 수만 건) 목업 유지.
            // 실제 연동 시에는 전용 집계(주/월/년) API 응답을 받아 동일하게 매핑하면 됨
            AdminDataUiState.Period.WEEK,
            AdminDataUiState.Period.MONTH,
            AdminDataUiState.Period.YEAR -> longRangeValues(category, period)
        }
    }

    private fun averageFor(category: AdminDataUiState.Category, predicate: (MetricLog) -> Boolean): Float {
        val values = metricLogs.filter(predicate).mapNotNull { it.valueFor(category) }
        return if (values.isEmpty()) 0f else values.average().toFloat()
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
