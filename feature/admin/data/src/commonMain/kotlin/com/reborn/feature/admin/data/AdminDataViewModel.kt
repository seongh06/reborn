package com.reborn.feature.admin.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.domain.usecase.ExportMetricToSheetsUseCase
import com.reborn.core.domain.usecase.GetAnalysisTextParams
import com.reborn.core.domain.usecase.GetAnalysisTextUseCase
import com.reborn.core.domain.usecase.GetDeviceListUseCase
import com.reborn.core.domain.usecase.GetGoogleSheetsAuthorizeUrlUseCase
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.core.domain.usecase.GetSensorAggregateParams
import com.reborn.core.domain.usecase.GetSensorAggregateUseCase
import com.reborn.core.domain.usecase.GetSensorHistoryParams
import com.reborn.core.domain.usecase.GetSensorHistoryUseCase
import com.reborn.core.model.SensorPoint
import com.reborn.feature.admin.data.model.AdminDataIntent
import com.reborn.feature.admin.data.model.AdminDataUiState
import com.reborn.feature.admin.data.model.MIN_ANALYSIS_DATA_COUNT
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

private val METRIC_CAPABLE_DEVICE_TYPES = setOf("ARDUINO", "SMART_THINGS")

// 목업 시간별 히스토리("어제"/"오늘")의 실제 일수. hourlyDayPatternsFor의 패턴 개수와 항상 같이 맞춰서 사용
private const val MOCK_HISTORY_DAY_COUNT = 2

private val DAYS_IN_MONTH = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

private data class MockDate(val year: Int, val month: Int, val day: Int)

private val today: MockDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    .let { MockDate(it.year, it.monthNumber, it.dayOfMonth) }

// 오늘 몫 HOUR 라벨을 현재 시각까지만 만들기 위한 기준 - 미래 시간대까지 라벨을 만들면
// 실제 값(SensorHistoryApiImpl에서도 동일하게 현재 시각까지만 채움)과 어긋나 그래프 끝이
// 밤(23시)까지 억지로 이어지며 0으로 뚝 떨어져 보인다.
private val currentHour: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour

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
        listOf(
            18.0, 17.5, 17.0, 17.0, 17.5, 18.0, 19.0, 20.5,
            22.0, 23.5, 24.5, 25.0, 25.5, 26.0, 25.5, 25.0,
            24.0, 23.0, 22.0, 21.0, 20.0, 19.5, 19.0, 18.5
        ),
        listOf(
            19.0, 18.5, 18.0, 18.5, 19.5, 21.0, 22.5, 24.0,
            25.5, 26.5, 27.0, 27.5, 28.0, 27.5, 27.0, 26.0,
            25.0, 23.5, 22.0, 21.0, 20.5, 20.0, 19.5, 19.0
        )
    )
    AdminDataUiState.Category.HUMIDITY -> listOf(
        listOf(
            68.0, 69.0, 70.0, 71.0, 70.0, 68.0, 65.0, 60.0,
            55.0, 50.0, 47.0, 45.0, 44.0, 43.0, 45.0, 47.0,
            50.0, 54.0, 58.0, 61.0, 64.0, 66.0, 67.0, 68.0
        ),
        listOf(
            60.0, 61.0, 62.0, 63.0, 62.0, 60.0, 57.0, 53.0,
            49.0, 45.0, 42.0, 40.0, 39.0, 39.0, 41.0, 43.0,
            46.0, 50.0, 54.0, 57.0, 58.0, 59.0, 60.0, 60.0
        )
    )
    AdminDataUiState.Category.ILLUMINANCE -> listOf(
        listOf(
            0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 80.0, 200.0,
            350.0, 500.0, 620.0, 700.0, 720.0, 700.0, 650.0, 560.0,
            420.0, 280.0, 120.0, 40.0, 5.0, 0.0, 0.0, 0.0
        ),
        listOf(
            0.0, 0.0, 0.0, 0.0, 10.0, 40.0, 120.0, 260.0,
            400.0, 540.0, 650.0, 730.0, 750.0, 730.0, 680.0, 590.0,
            450.0, 300.0, 140.0, 50.0, 10.0, 0.0, 0.0, 0.0
        )
    )
    AdminDataUiState.Category.PEOPLE_COUNT -> listOf(
        listOf(
            0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 2.0,
            3.0, 4.0, 4.0, 5.0, 5.0, 4.0, 4.0, 3.0,
            3.0, 4.0, 5.0, 6.0, 4.0, 2.0, 1.0, 0.0
        ),
        listOf(
            1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0,
            3.0, 3.0, 4.0, 4.0, 5.0, 5.0, 4.0, 3.0,
            3.0, 4.0, 6.0, 7.0, 5.0, 3.0, 1.0, 1.0
        )
    )
    AdminDataUiState.Category.DISCOMFORT -> listOf(
        listOf(
            58.0, 57.0, 56.0, 56.0, 57.0, 59.0, 62.0, 66.0,
            70.0, 74.0, 77.0, 79.0, 80.0, 81.0, 80.0, 78.0,
            75.0, 71.0, 68.0, 65.0, 63.0, 61.0, 60.0, 59.0
        ),
        listOf(
            60.0, 59.0, 58.0, 58.0, 59.0, 61.0, 64.0, 68.0,
            72.0, 76.0, 79.0, 81.0, 82.0, 83.0, 82.0, 80.0,
            77.0, 73.0, 70.0, 67.0, 65.0, 63.0, 62.0, 61.0
        )
    )
}

sealed class AdminDataEvent {
    data object Exit : AdminDataEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminDataEvent()
    data class ShowSnackbar(val message: String) : AdminDataEvent()
    data class OpenUrl(val url: String) : AdminDataEvent()
}

class AdminDataViewModel(
    private val getPlaceListUseCase: GetPlaceListUseCase,
    private val getDeviceListUseCase: GetDeviceListUseCase,
    private val getSensorHistoryUseCase: GetSensorHistoryUseCase,
    private val getSensorAggregateUseCase: GetSensorAggregateUseCase,
    private val getAnalysisTextUseCase: GetAnalysisTextUseCase,
    private val exportMetricToSheetsUseCase: ExportMetricToSheetsUseCase,
    private val getGoogleSheetsAuthorizeUrlUseCase: GetGoogleSheetsAuthorizeUrlUseCase,
) : ViewModel() {
    private val navigationManager = NavigationManager<AdminDataUiState, AdminDataEvent>(
        initialState = AdminDataUiState.Loading,
        exitEvent = AdminDataEvent.Exit,
        scope = viewModelScope
    )

    private var loadJob: Job? = null

    // 분석 결과 로딩은 차트/카테고리/기간 로딩(loadJob)과 완전히 독립된 작업이라 같은 Job을
    // 공유하면 한쪽이 취소될 때 다른 쪽도 조용히 취소된다(CodeRabbit 리뷰) - 별도 Job으로 분리.
    private var analysisJob: Job? = null

    // TODO: 장소 선택/전환 개념이 앱에 아직 없어(#166 참고) 첫 번째 장소의 첫 ARDUINO/SMART_THINGS
    // 기기로 임시 고정한다.
    private var resolvedDeviceId: String? = null
    private var hasAerometer: Boolean = false
    private var deviceContextResolved: Boolean = false

    // 장소의 기기 목록을 조회해 (조회/제어 대상 deviceId, 공기계 연결 여부)를 함께 얻는다 - 조도/재실
    // 인원 탭은 공기계(AEROMETER)가 있어야만 노출해야 하므로(#236) deviceId만 필요했던 이전 로직에
    // 공기계 존재 여부도 같이 계산한다. 실패 시 캐시를 지워 다음 진입 때 다시 조회한다(#232와 동일한
    // 이유 - 캐시가 죽은 채로 남아있으면 장소가 삭제된 뒤에도 계속 실패한다).
    private suspend fun resolveDeviceContext(): String? {
        if (deviceContextResolved) return resolvedDeviceId
        val placeId = getPlaceListUseCase().getOrNull()?.firstOrNull()?.placeId
        if (placeId == null) {
            deviceContextResolved = true
            return null
        }
        val devices = getDeviceListUseCase(placeId).getOrNull()
        if (devices == null) return null
        resolvedDeviceId = devices.firstOrNull { it.deviceType in METRIC_CAPABLE_DEVICE_TYPES }?.deviceId
        hasAerometer = devices.any { it.deviceType == "AEROMETER" }
        deviceContextResolved = true
        return resolvedDeviceId
    }

    private suspend fun resolveDeviceId(): String? = resolveDeviceContext()

    private fun availableCategories(): List<AdminDataUiState.Category> =
        if (hasAerometer) {
            AdminDataUiState.Category.entries
        } else {
            AdminDataUiState.Category.entries.filterNot {
                it == AdminDataUiState.Category.ILLUMINANCE || it == AdminDataUiState.Category.PEOPLE_COUNT
            }
        }

    val uiState = navigationManager.uiState
    val event = navigationManager.event

    fun onIntent(intent: AdminDataIntent) {
        when (intent) {
            is AdminDataIntent.LoadInitial -> checkInitialState()
            is AdminDataIntent.ClickCategoryTab -> handleCategoryClick(intent.category)
            is AdminDataIntent.ClickPeriod -> handlePeriodClick(intent.period)
            is AdminDataIntent.ClickExport -> exportToGoogleSheets()
            is AdminDataIntent.ClickRevealAnalysis -> revealAnalysis()
        }
    }

    private fun checkInitialState() {
        navigationManager.clearAndReset(AdminDataUiState.Loading)
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val category = AdminDataUiState.Category.TEMPERATURE
            val period = AdminDataUiState.Period.DAY
            try {
                resolveDeviceContext()
                navigationManager.clearAndReset(
                    AdminDataUiState.Data(
                        selectedCategory = category,
                        selectedPeriod = period,
                        chartLabels = chartLabelsFor(period),
                        chartValues = chartValuesFor(category, period),
                        hasEnoughData = hasEnoughDataFor(period),
                        availableCategories = availableCategories(),
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                navigationManager.emitEvent(AdminDataEvent.ShowErrorSnackbar(e))
            }
        }
    }

    private fun handleCategoryClick(category: AdminDataUiState.Category) {
        val current = navigationManager.uiState.value as? AdminDataUiState.Data ?: return
        loadJob?.cancel()
        // 진행 중인 분석 요청이 있다면 취소 - 그대로 두면 카테고리를 바꾼 뒤에 이전 카테고리의
        // 분석 결과가 뒤늦게 도착해 새 상태에 잘못 덮어써질 수 있다.
        analysisJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val chartValues = chartValuesFor(category, current.selectedPeriod)
                navigationManager.updateCurrentState { state ->
                    if (state is AdminDataUiState.Data) {
                        state.copy(
                            selectedCategory = category,
                            chartValues = chartValues,
                            // 카테고리가 바뀌면 이전 분석 결과는 더 이상 맞지 않으니 다시 잠금 상태로
                            analysisText = null,
                            isAnalysisLoading = false,
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
        analysisJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val chartValues = chartValuesFor(current.selectedCategory, period)
                navigationManager.updateCurrentState { state ->
                    if (state is AdminDataUiState.Data) {
                        state.copy(
                            selectedPeriod = period,
                            chartLabels = chartLabelsFor(period),
                            chartValues = chartValues,
                            hasEnoughData = hasEnoughDataFor(period),
                            // 기간이 바뀌면 데이터 개수 자체가 달라지니 분석 결과도 다시 잠금 상태로
                            analysisText = null,
                            isAnalysisLoading = false,
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

    // 분석 결과 블러 카드를 탭했을 때만 Gemini를 호출한다 - 자동 호출을 없애 토큰 낭비를 막는다.
    // 데이터가 충분하지 않으면(MIN_ANALYSIS_DATA_COUNT 미만) 애초에 화면에서 탭 자체가 안 뜨지만,
    // 방어적으로 서비스 레이어에서도 한 번 더 막는다.
    private fun revealAnalysis() {
        val current = navigationManager.uiState.value as? AdminDataUiState.Data ?: return
        if (current.isAnalysisLoading || current.analysisText != null) return
        if (current.chartValues.size < MIN_ANALYSIS_DATA_COUNT) return

        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            navigationManager.updateCurrentState { state ->
                if (state is AdminDataUiState.Data) state.copy(isAnalysisLoading = true) else state
            }
            try {
                val analysisText = fetchAnalysisText(current.selectedCategory)
                navigationManager.updateCurrentState { state ->
                    if (state is AdminDataUiState.Data) {
                        state.copy(analysisText = analysisText, isAnalysisLoading = false)
                    } else state
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                navigationManager.updateCurrentState { state ->
                    if (state is AdminDataUiState.Data) state.copy(isAnalysisLoading = false) else state
                }
                navigationManager.emitEvent(AdminDataEvent.ShowErrorSnackbar(e))
            }
        }
    }

    // 기기가 등록된 지 얼마 안 돼서 해당 기간 단위로 충분한 데이터가 쌓이지 않았으면 그래프 대신 안내 문구를 보여주기 위한 판단
    // (1시간은 오늘 하루치라 항상 표시, 일/주/월/년은 각각 최소 6개 단위만큼 쌓였을 때만 표시)
    // 일(DAY)은 실제 목업 히스토리 일수(MOCK_HISTORY_DAY_COUNT)로, 주/월/년은 데이터 수집 시작일로부터 경과한 시간으로 판단
    //
    // 주의: 지금 목업 상수(MOCK_HISTORY_DAY_COUNT=2, 경과일=약 33일)로는 HOUR를 제외한 나머지가 전부
    // "데이터 없음" 상태가 되도록 의도된 것 — 갓 등록된 기기가 아직 장기 데이터를 못 쌓은 상황을 보여주기 위함.
    // 사용자 요청으로 명시적으로 이렇게 맞춘 것이라 CodeRabbit이 제안한 "임계값 완화"는 적용하지 않음.
    // 목업 "오늘" 날짜가 이후 세션에서 전진하면 자연스럽게 WEEK(6주)/MONTH(6개월)/YEAR(6년) 순으로 열리게 됨
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
    //
    // 주의: HOUR/DAY의 라벨은 여전히 이 2일(MOCK_HISTORY_DAY_COUNT) 가정으로 생성된다. 실 데이터
    // (hourlyValues/dailyAverageValues)가 실제로 몇 일치를 반환하는지는 기기 수집 이력에 따라
    // 달라질 수 있어 정확히 일치하지 않을 수 있음 - 차트 컴포넌트가 labels.getOrNull()로 범위를
    // 벗어난 인덱스는 라벨 없이 넘어가도록 이미 방어하고 있어 크래시 없이 일부 라벨만 비게 되는
    // 정도의 코스메틱 이슈로 그침. 실 수집 이력 기준으로 라벨을 동적 생성하는 건 별도 개선 필요.
    private fun chartLabelsFor(period: AdminDataUiState.Period): List<String> {
        return when (period) {
            // 1시간 간격 · 목업이 딱 2일치(어제/오늘)라 그 2일을 이어서 표시. 자정(0시)엔 "HH:00" 대신 그날 날짜(ex. "3일")로 표시해 날짜가 바뀌었음을 알림
            AdminDataUiState.Period.HOUR -> (MOCK_HISTORY_DAY_COUNT - 1 downTo 0).flatMap { daysAgo ->
                val date = today.minusDays(daysAgo)
                val lastHour = if (daysAgo == 0) currentHour else 23
                (0..lastHour).map { hour -> if (hour == 0) "${date.day}일" else "${hour.pad2()}:00" }
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

    // HOUR/DAY는 category==DISCOMFORT일 때만 목업 유지(서버 히스토리 API에 불쾌지수 필드 자체가
    // 없음) - 나머지 4개 카테고리는 실 기기가 있으면 실 데이터를 쓰고, 없으면 빈 그래프를 반환한다
    // (라벨-값 개수가 어긋나지 않도록 빈 값일 땐 라벨도 비워서 호출부에서 함께 처리)
    // WEEK/MONTH/YEAR는 서버 전용 집계 API(#158)를 사용 - DISCOMFORT도 서버가 실제로 계산해 내려줌.
    private suspend fun chartValuesFor(category: AdminDataUiState.Category, period: AdminDataUiState.Period): List<Float> {
        if (!hasEnoughDataFor(period)) return emptyList()
        return when (period) {
            AdminDataUiState.Period.HOUR -> hourlyValues(category)
            AdminDataUiState.Period.DAY -> dailyAverageValues(category)
            AdminDataUiState.Period.WEEK,
            AdminDataUiState.Period.MONTH,
            AdminDataUiState.Period.YEAR -> aggregateValues(category, period)
        }
    }

    private suspend fun hourlyValues(category: AdminDataUiState.Category): List<Float> {
        if (category == AdminDataUiState.Category.DISCOMFORT) return mockDiscomfortPoints().map { it.value.toFloat() }
        val deviceId = resolveDeviceId() ?: return emptyList()
        val params = GetSensorHistoryParams(deviceId, category.name)
        return getSensorHistoryUseCase(params).first().map { point -> point.value.toFloat() }
    }

    private suspend fun dailyAverageValues(category: AdminDataUiState.Category): List<Float> {
        val points = if (category == AdminDataUiState.Category.DISCOMFORT) {
            mockDiscomfortPoints()
        } else {
            val deviceId = resolveDeviceId() ?: return emptyList()
            getSensorHistoryUseCase(GetSensorHistoryParams(deviceId, category.name)).first()
        }
        return points.groupBy { it.date }
            .toList()
            .sortedBy { (date, _) -> date }
            .map { (_, dayPoints) -> dayPoints.map { point -> point.value }.average().toFloat() }
    }

    // DISCOMFORT는 서버 히스토리 API에 없는 필드라 목업 그대로 유지 - 예전 MockSensorHistoryApi가
    // 하던 변환(리터럴 패턴 -> 날짜별 SensorPoint)을 API 계층 없이 직접 재현
    private fun mockDiscomfortPoints(): List<SensorPoint> {
        val patterns = hourlyDayPatternsFor(AdminDataUiState.Category.DISCOMFORT)
        return patterns.indices.flatMap { index ->
            val daysAgo = patterns.size - 1 - index
            val date = today.minusDays(daysAgo)
            // 오늘 몫은 현재 시각까지만 - 나머지 카테고리(실 데이터)와 동일하게 미래 시간대는 만들지 않는다.
            val lastHour = if (daysAgo == 0) currentHour else 23
            patterns[index].mapIndexedNotNull { hour, value ->
                if (hour > lastHour) null else SensorPoint(date = date.toDateKey(), hour = hour, value = value)
            }
        }
    }

    private suspend fun aggregateValues(category: AdminDataUiState.Category, period: AdminDataUiState.Period): List<Float> {
        val deviceId = resolveDeviceId() ?: return emptyList()
        val params = GetSensorAggregateParams(deviceId, category.name, period.name)
        return getSensorAggregateUseCase(params).first()
    }

    // Gemini 호출 실패는 여기서 문구로 삼키지 않고 그대로 던진다 - revealAnalysis()의 catch가
    // analysisText를 null로 유지하고 에러 스낵바를 띄워야 사용자가 블러 카드를 다시 탭해
    // 재시도할 수 있다(CodeRabbit 리뷰) - 삼키면 실패 문구가 "AI로 생성된 문구"로 굳어버린다.
    private suspend fun fetchAnalysisText(category: AdminDataUiState.Category): String {
        val deviceId = resolveDeviceId() ?: return "아직 등록된 기기가 없어 분석할 수 없습니다."
        return getAnalysisTextUseCase(GetAnalysisTextParams(deviceId, category.name)).first()
    }

    private fun exportToGoogleSheets() {
        viewModelScope.launch {
            val deviceId = resolveDeviceId()
            if (deviceId == null) {
                navigationManager.emitEvent(AdminDataEvent.ShowErrorSnackbar(IllegalStateException("등록된 기기가 없어 내보낼 데이터가 없습니다.")))
                return@launch
            }
            try {
                val spreadsheetUrl = exportMetricToSheetsUseCase(deviceId).first()
                navigationManager.emitEvent(AdminDataEvent.ShowSnackbar("Google Sheets로 내보냈습니다."))
                navigationManager.emitEvent(AdminDataEvent.OpenUrl(spreadsheetUrl))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 장소가 Google Sheets와 아직 연동되지 않은 경우(서버 404) - 바로 연동 화면으로
                // 안내해서 한 번 더 시도할 필요 없이 이어서 연동할 수 있게 한다.
                if (e.message?.contains("연동되어 있지 않습니다") == true) {
                    startGoogleSheetsConnection()
                } else {
                    navigationManager.emitEvent(AdminDataEvent.ShowErrorSnackbar(e))
                }
            }
        }
    }

    private suspend fun startGoogleSheetsConnection() {
        val placeId = getPlaceListUseCase().getOrNull()?.firstOrNull()?.placeId
        if (placeId == null) {
            navigationManager.emitEvent(AdminDataEvent.ShowErrorSnackbar(IllegalStateException("등록된 장소가 없습니다.")))
            return
        }
        getGoogleSheetsAuthorizeUrlUseCase(placeId)
            .onSuccess { url ->
                navigationManager.emitEvent(AdminDataEvent.ShowSnackbar("Google Sheets 연동이 필요해요. 연동 후 다시 내보내기를 눌러주세요."))
                navigationManager.emitEvent(AdminDataEvent.OpenUrl(url))
            }
            .onFailure { navigationManager.emitEvent(AdminDataEvent.ShowErrorSnackbar(it)) }
    }
}
