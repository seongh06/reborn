package com.reborn.feature.admin.home.model

import androidx.compose.runtime.Immutable
import com.reborn.core.model.Metric
import com.reborn.core.ui.component.FeedbackListItem
import com.reborn.core.ui.component.FeedbackType
import com.reborn.core.ui.component.RoomOption
import com.reborn.feature.admin.home.component.IoTDeviceItem

@Immutable
sealed interface AdminHomeUiState{
    data object Loading: AdminHomeUiState
    data class Home(
        val hasDevices: Boolean = true,
        val devices: List<IoTDeviceItem> = emptyList(),
        val metric: Metric? = null,
        val feedbackTotalCount: Int = 0,
        val feedbackWaitingCount: Int = 0,
        val recentFeedbacks: List<FeedbackListItem> = emptyList(),
        // 최초 접속 튜토리얼(#240) - 기기가 하나도 없는 신규 사용자에게 SmartThings 연결
        // 진입점을 하이라이트로 안내한다.
        val showTutorialHint: Boolean = false,
        // 최초 접속 튜토리얼(#240) - 첫 피드백이 도착했을 때 "실시간 피드백" 섹션을 하이라이트로
        // 안내한다. 별도의 0->1 전이 추적 없이 "피드백이 1건 이상 && 이 단계를 아직 안 봄"으로
        // 판단 - 다른 단계들과 동일한 패턴(현재 상태 + 미확인 여부).
        val showFirstFeedbackHint: Boolean = false,
        // 룸 전환(#166) - 타이틀에 표시할 룸 목록/현재 선택된 룸. rooms.size <= 1이면 화면에서
        // "Re:Born"으로 표시하고, 2개 이상이면 selectedRoomId에 해당하는 룸 이름을 표시한다.
        val rooms: List<RoomOption> = emptyList(),
        val selectedRoomId: Long? = null,
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
        val time: String? = null,
        // 피드백 타입일 때만 채워짐 - 알림 목록 앞 동그라미에 피드백 목록과 동일한 종류별 아이콘을 쓰기 위함.
        // 결산(SETTLEMENT)은 매칭되는 아이콘이 없어 null로 남겨 회색 빈 동그라미로 표시.
        val feedbackType: FeedbackType? = null,
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
    data object NavigateToAddSmartThingsDevice : AdminHomeIntent
    data class NavigateToDeviceDetail(val deviceId: String) : AdminHomeIntent
    data class TogglePower(val deviceId: String) : AdminHomeIntent
    data object NavigateBack : AdminHomeIntent
    data class NavigateToFeedback(val feedbackId: Int): AdminHomeIntent
    data object NavigateToFeedbackList : AdminHomeIntent
    data class DeleteAlarm(val alarmId: Int) : AdminHomeIntent
    data class ClickAlarmFilter(val filter: AdminHomeUiState.AlarmFilter) : AdminHomeIntent
    data class DismissTutorial(val stepId: String) : AdminHomeIntent
    data class SelectPlace(val placeId: Long) : AdminHomeIntent
}