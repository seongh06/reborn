package com.reborn.feature.admin.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.domain.usecase.DeletePlaceUseCase
import com.reborn.core.domain.usecase.GetPlaceDetailUseCase
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.core.domain.usecase.GetUserProfileUseCase
import com.reborn.core.domain.usecase.LogoutUseCase
import com.reborn.core.domain.usecase.UpdateUserProfileUseCase
import com.reborn.core.domain.usecase.WithdrawUseCase
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
    data class NavigateToAddAiSpeaker(val placeId: Int) : AdminSettingEvent()
    data object NavigateToAddPlace : AdminSettingEvent()
    data object LoggedOut : AdminSettingEvent()
}

class AdminSettingViewModel(
    private val logoutUseCase: LogoutUseCase,
    private val getPlaceListUseCase: GetPlaceListUseCase,
    private val getPlaceDetailUseCase: GetPlaceDetailUseCase,
    private val deletePlaceUseCase: DeletePlaceUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val withdrawUseCase: WithdrawUseCase,
) : ViewModel() {
    private val navigationManager = NavigationManager<AdminSettingUiState, AdminSettingEvent>(
        initialState = AdminSettingUiState.Loading,
        exitEvent = AdminSettingEvent.Exit,
        scope = viewModelScope
    )

    val uiState = navigationManager.uiState
    val event = navigationManager.event

    private var isLoggingOut = false

    // checkInitialState()의 병렬 프로필 조회가 늦게 끝나면 그 사이 사용자가 편집해 성공한 이름을
    // 덮어쓸 수 있어(CodeRabbit #179), 편집 성공 시점 이후로는 이 값을 조회 결과보다 우선한다.
    private var editedProfileName: String? = null

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
            is AdminSettingIntent.ClickAddAiSpeaker ->
                navigationManager.emitEvent(AdminSettingEvent.NavigateToAddAiSpeaker(intent.placeId))
            is AdminSettingIntent.ClickAddPlace -> navigationManager.emitEvent(AdminSettingEvent.NavigateToAddPlace)
            is AdminSettingIntent.ClickLogout -> logout()
            is AdminSettingIntent.ClickWithdraw -> withdraw()
            is AdminSettingIntent.UpdateProfileName -> updateProfileName(intent.name)
        }
    }

    private fun updateProfileName(name: String) {
        viewModelScope.launch {
            updateUserProfileUseCase(name)
                .onSuccess { profile ->
                    editedProfileName = profile.name
                    navigationManager.updateCurrentState { state ->
                        (state as? AdminSettingUiState.Setting)?.copy(profileName = profile.name) ?: state
                    }
                }
                .onFailure { navigationManager.emitEvent(AdminSettingEvent.ShowErrorSnackbar(it)) }
        }
    }

    private fun checkInitialState() {
        navigationManager.clearAndReset(AdminSettingUiState.Loading)
        viewModelScope.launch {
            // 프로필 조회(#155)는 장소 목록과 무관한 별도 API - 여기서 병렬로 시작해두되, 그 결과를
            // 기다리는 시점을 장소 목록이 이미 화면에 반영된 뒤로 미뤄서 장소 목록 표시를 지연시키지 않는다.
            val profile = async { getUserProfileUseCase().getOrNull() }

            fun applyProfileWhenReady() {
                viewModelScope.launch {
                    val userProfile = profile.await() ?: return@launch
                    navigationManager.updateCurrentState { state ->
                        (state as? AdminSettingUiState.Setting)
                            ?.copy(
                                profileName = editedProfileName ?: userProfile.name,
                                profileImageUrl = userProfile.profileImage
                            )
                            ?: state
                    }
                }
            }

            getPlaceListUseCase()
                .onSuccess { places ->
                    // 장소 목록(#27)에는 deviceCount가 없어 장소별로 상세(#28)를 추가 조회해 채운다.
                    // 장소 수만큼 한 번에 요청이 나가지 않도록 동시 조회 수를 제한한다.
                    val detailSemaphore = Semaphore(MAX_CONCURRENT_DETAIL_REQUESTS)
                    val rooms = coroutineScope {
                        places.map { place ->
                            async {
                                val detail = detailSemaphore.withPermit {
                                    getPlaceDetailUseCase(place.placeId).getOrNull()
                                }
                                AdminSettingUiState.RoomItem(
                                    // Route 인자(Route.Admin.InviteCode/AddDevice)가 Int라 기존 관례를 따라 Int로 보관
                                    placeId = place.placeId.toInt(),
                                    roomName = place.name,
                                    // 상세 조회 실패 시 null - 실제 0명/0대와 구분해서 UI에서 별도 표시
                                    adminCount = detail?.adminCount,
                                    deviceCount = detail?.deviceCount,
                                )
                            }
                        }.awaitAll()
                    }
                    navigationManager.clearAndReset(AdminSettingUiState.Setting(rooms = rooms))
                    applyProfileWhenReady()
                }
                .onFailure {
                    navigationManager.emitEvent(AdminSettingEvent.ShowErrorSnackbar(it))
                    navigationManager.clearAndReset(AdminSettingUiState.Setting(rooms = emptyList()))
                    applyProfileWhenReady()
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

    private fun withdraw() {
        if (isLoggingOut) return
        isLoggingOut = true
        viewModelScope.launch {
            withdrawUseCase()
                .onSuccess {
                    // 탈퇴 후에도 인트로로 빠져나가는 동작은 로그아웃과 동일해서 이벤트를 재사용
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
