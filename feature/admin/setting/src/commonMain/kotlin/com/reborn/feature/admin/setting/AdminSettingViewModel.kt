package com.reborn.feature.admin.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.feature.admin.setting.model.AdminSettingIntent
import com.reborn.feature.admin.setting.model.AdminSettingUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class AdminSettingEvent {
    data object Exit : AdminSettingEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminSettingEvent()
    data class ShowSnackbar(val message: String) : AdminSettingEvent()
    data class NavigateToInviteCode(val placeId: Int) : AdminSettingEvent()
    data class NavigateToAddDevice(val placeId: Int) : AdminSettingEvent()
    data object NavigateToAddPlace : AdminSettingEvent()
}

class AdminSettingViewModel : ViewModel() {
    private val navigationManager = NavigationManager<AdminSettingUiState, AdminSettingEvent>(
        initialState = AdminSettingUiState.Loading,
        exitEvent = AdminSettingEvent.Exit,
        scope = viewModelScope
    )

    val uiState = navigationManager.uiState
    val event = navigationManager.event

    // TODO: 서버 place 목록 조회 API 연동 전까지의 목업 데이터. 실제 연동 시 UseCase로 대체 예정
    private var rooms: List<AdminSettingUiState.RoomItem> = listOf(
        AdminSettingUiState.RoomItem(placeId = 1, roomName = "Home 01", adminCount = 3, deviceCount = 3),
        AdminSettingUiState.RoomItem(placeId = 2, roomName = "Cafe", adminCount = 2, deviceCount = 5)
    )

    fun onIntent(intent: AdminSettingIntent) {
        when (intent) {
            is AdminSettingIntent.LoadInitial -> checkInitialState()
            is AdminSettingIntent.NavigateBack -> navigationManager.navigateBack()
            is AdminSettingIntent.DeleteRoom -> deleteRoom(intent.placeId)
            is AdminSettingIntent.ClickAddAdmin ->
                navigationManager.emitEvent(AdminSettingEvent.NavigateToInviteCode(intent.placeId))
            is AdminSettingIntent.ClickAddDevice ->
                navigationManager.emitEvent(AdminSettingEvent.NavigateToAddDevice(intent.placeId))
            is AdminSettingIntent.ClickAddPlace -> navigationManager.emitEvent(AdminSettingEvent.NavigateToAddPlace)
        }
    }

    private fun checkInitialState() {
        navigationManager.clearAndReset(AdminSettingUiState.Loading)
        viewModelScope.launch {
            delay(500)
            navigationManager.clearAndReset(AdminSettingUiState.Setting(rooms = rooms))
        }
    }

    private fun deleteRoom(placeId: Int) {
        rooms = rooms.filterNot { it.placeId == placeId }
        navigationManager.updateCurrentState { state ->
            (state as? AdminSettingUiState.Setting)?.copy(rooms = rooms) ?: state
        }
    }
}
