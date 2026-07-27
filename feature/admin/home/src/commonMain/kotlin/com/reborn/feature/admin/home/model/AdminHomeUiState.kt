package com.reborn.feature.admin.home.model

import androidx.compose.runtime.Immutable
import com.reborn.core.model.Metric
import com.reborn.core.ui.component.FeedbackListItem

@Immutable
sealed interface AdminHomeUiState{
    data object Loading: AdminHomeUiState
    data class Home(
        // TODO: 서버 device API 연동 전까지는 항상 true 취급(기기 그리드 자체는 여전히 목업, #166/#134
        // 참고 - 방 그룹핑·파워 상태를 목록 API가 안 내려줘서 이번 범위 밖). 기기가 하나도 없는 실제
        // 케이스만 device 목록 조회 결과로 정확히 반영.
        val hasDevices: Boolean = true,
        val metric: Metric? = null,
        val feedbackTotalCount: Int = 0,
        val feedbackWaitingCount: Int = 0,
        val recentFeedbacks: List<FeedbackListItem> = emptyList()
    ): AdminHomeUiState
    data class Alarm(
        val alarm: List<AlarmItem> = emptyList(),
        val filter: AlarmFilter = AlarmFilter.ALL
    ): AdminHomeUiState

    // group: Figma 596:5094 "이번 주"/"이전" 구획, category: 필터칩(전체/피드백/결산) 대상.
    // title/time은 결산(SETTLEMENT) 타입에만 있음 - 피드백 타입은 content 한 줄뿐(Figma 595:5108)
    data class AlarmItem(
        val id: Int,
        val group: AlarmGroup,
        val category: AlarmFilter,
        val content: String,
        val title: String? = null,
        val time: String? = null
    )

    enum class AlarmGroup(val label: String) {
        THIS_WEEK("이번 주"),
        PREVIOUS("이전")
    }

    enum class AlarmFilter(val label: String) {
        ALL("전체"),
        FEEDBACK("피드백"),
        SETTLEMENT("결산")
    }
}

fun AdminHomeUiState.Alarm.filteredGroupedAlarms(): Map<AdminHomeUiState.AlarmGroup, List<AdminHomeUiState.AlarmItem>> {
    val filtered = if (filter == AdminHomeUiState.AlarmFilter.ALL) alarm else alarm.filter { it.category == filter }
    return AdminHomeUiState.AlarmGroup.entries.associateWith { group ->
        filtered.filter { it.group == group }
    }.filterValues { it.isNotEmpty() }
}

sealed interface AdminHomeIntent{
    data object LoadInitial : AdminHomeIntent
    data object NavigateToAlarm : AdminHomeIntent
    data object NavigateToSetting : AdminHomeIntent
    data object NavigateToDeviceList : AdminHomeIntent
    data object NavigateBack : AdminHomeIntent
    data class NavigateToFeedback(val feedbackId: Int): AdminHomeIntent
    data object NavigateToFeedbackList : AdminHomeIntent
    data class DeleteAlarm(val alarmId: Int) : AdminHomeIntent
    data class ClickAlarmFilter(val filter: AdminHomeUiState.AlarmFilter) : AdminHomeIntent
}