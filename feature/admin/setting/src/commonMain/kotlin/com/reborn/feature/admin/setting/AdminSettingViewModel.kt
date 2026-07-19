package com.reborn.feature.admin.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.domain.usecase.DeletePlaceUseCase
import com.reborn.core.domain.usecase.GetPlaceDetailUseCase
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.core.domain.usecase.LogoutUseCase
import com.reborn.feature.admin.setting.model.AdminSettingIntent
import com.reborn.feature.admin.setting.model.AdminSettingUiState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

sealed class AdminSettingEvent {
    data object Exit : AdminSettingEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminSettingEvent()
    data class ShowSnackbar(val message: String) : AdminSettingEvent()
    data class NavigateToInviteCode(val placeId: Int) : AdminSettingEvent()
    data class NavigateToAddDevice(val placeId: Int) : AdminSettingEvent()
    data class NavigateToAddArduino(val placeId: Int) : AdminSettingEvent()
    data object NavigateToAddPlace : AdminSettingEvent()
    data object LoggedOut : AdminSettingEvent()
}

class AdminSettingViewModel(
    private val logoutUseCase: LogoutUseCase,
    private val getPlaceListUseCase: GetPlaceListUseCase,
    private val getPlaceDetailUseCase: GetPlaceDetailUseCase,
    private val deletePlaceUseCase: DeletePlaceUseCase,
) : ViewModel() {
    private val navigationManager = NavigationManager<AdminSettingUiState, AdminSettingEvent>(
        initialState = AdminSettingUiState.Loading,
        exitEvent = AdminSettingEvent.Exit,
        scope = viewModelScope
    )

    val uiState = navigationManager.uiState
    val event = navigationManager.event

    private var isLoggingOut = false

    fun onIntent(intent: AdminSettingIntent) {
        when (intent) {
            is AdminSettingIntent.LoadInitial -> checkInitialState()
            is AdminSettingIntent.NavigateBack -> navigationManager.navigateBack()
            is AdminSettingIntent.DeleteRoom -> deleteRoom(intent.placeId)
            is AdminSettingIntent.ClickAddAdmin ->
                navigationManager.emitEvent(AdminSettingEvent.NavigateToInviteCode(intent.placeId))
            is AdminSettingIntent.ClickAddDevice ->
                navigationManager.emitEvent(AdminSettingEvent.NavigateToAddDevice(intent.placeId))
            is AdminSettingIntent.ClickAddArduino ->
                navigationManager.emitEvent(AdminSettingEvent.NavigateToAddArduino(intent.placeId))
            is AdminSettingIntent.ClickAddPlace -> navigationManager.emitEvent(AdminSettingEvent.NavigateToAddPlace)
            is AdminSettingIntent.ClickLogout -> logout()
        }
    }

    private fun checkInitialState() {
        navigationManager.clearAndReset(AdminSettingUiState.Loading)
        viewModelScope.launch {
            getPlaceListUseCase()
                .onSuccess { places ->
                    // 장소 목록(#27)에는 deviceCount가 없어 장소별로 상세(#28)를 추가 조회해 채운다.
                    // 장소 수만큼 한 번에 요청이 나가지 않도록 동시 조회 수를 제한한다.
                    val detailSemaphore = Semaphore(MAX_CONCURRENT_DETAIL_REQUESTS)
                    val rooms = coroutineScope {
                        places.map { place ->
                            async {
                                val deviceCount = detailSemaphore.withPermit {
                                    getPlaceDetailUseCase(place.placeId).getOrNull()?.deviceCount
                                }
                                AdminSettingUiState.RoomItem(
                                    // Route 인자(Route.Admin.InviteCode/AddDevice)가 Int라 기존 관례를 따라 Int로 보관
                                    placeId = place.placeId.toInt(),
                                    roomName = place.name,
                                    // TODO: 장소별 관리자 수를 반환하는 API가 아직 없어 0으로 표시 (#106 범위 밖)
                                    adminCount = 0,
                                    // 상세 조회 실패 시 null - 실제 대수 0과 구분해서 UI에서 별도 표시
                                    deviceCount = deviceCount,
                                )
                            }
                        }.awaitAll()
                    }
                    navigationManager.clearAndReset(AdminSettingUiState.Setting(rooms = rooms))
                }
                .onFailure {
                    navigationManager.emitEvent(AdminSettingEvent.ShowErrorSnackbar(it))
                    navigationManager.clearAndReset(AdminSettingUiState.Setting(rooms = emptyList()))
                }
        }
    }

    private fun deleteRoom(placeId: Int) {
        viewModelScope.launch {
            deletePlaceUseCase(placeId.toLong())
                .onSuccess {
                    navigationManager.updateCurrentState { state ->
                        (state as? AdminSettingUiState.Setting)
                            ?.copy(rooms = state.rooms.filterNot { it.placeId == placeId })
                            ?: state
                    }
                }
                .onFailure {
                    navigationManager.emitEvent(AdminSettingEvent.ShowErrorSnackbar(it))
                }
        }
    }

    private fun logout() {
        if (isLoggingOut) return
        isLoggingOut = true
        viewModelScope.launch {
            logoutUseCase()
                .onSuccess {
                    navigationManager.emitEvent(AdminSettingEvent.LoggedOut)
                }
                .onFailure {
                    isLoggingOut = false
                    navigationManager.emitEvent(AdminSettingEvent.ShowErrorSnackbar(it))
                }
        }
    }

    companion object {
        private const val MAX_CONCURRENT_DETAIL_REQUESTS = 5
    }
}
