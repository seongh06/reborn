package com.reborn.feature.admin.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.domain.usecase.DeletePlaceUseCase
import com.reborn.core.domain.usecase.GetPlaceAdminsUseCase
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.core.domain.usecase.GetUserProfileUseCase
import com.reborn.core.domain.usecase.LeavePlaceUseCase
import com.reborn.core.domain.usecase.LogoutUseCase
import com.reborn.core.domain.usecase.TransferPlaceOwnerUseCase
import com.reborn.core.domain.usecase.UpdateUserProfileImageUseCase
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
    private val getPlaceAdminsUseCase: GetPlaceAdminsUseCase,
    private val deletePlaceUseCase: DeletePlaceUseCase,
    private val leavePlaceUseCase: LeavePlaceUseCase,
    private val transferPlaceOwnerUseCase: TransferPlaceOwnerUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val updateUserProfileImageUseCase: UpdateUserProfileImageUseCase,
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

    // 이미지도 이름과 동일한 레이스가 있어(#179와 같은 클래스의 문제) 같은 방식으로 우선순위를 둔다.
    private var editedProfileImageUrl: String? = null

    fun onIntent(intent: AdminSettingIntent) {
        when (intent) {
            is AdminSettingIntent.LoadInitial -> checkInitialState()
            is AdminSettingIntent.NavigateBack -> navigationManager.navigateBack()
            is AdminSettingIntent.DeleteRoom -> deleteRoom(intent.placeId)
            is AdminSettingIntent.LeaveRoom -> leaveRoom(intent.placeId)
            is AdminSettingIntent.TransferOwner -> transferOwner(intent.placeId, intent.newOwnerUserId)
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
            is AdminSettingIntent.UpdateProfileImage ->
                updateProfileImage(intent.bytes, intent.fileName, intent.mimeType)
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

    private fun updateProfileImage(bytes: ByteArray, fileName: String, mimeType: String) {
        viewModelScope.launch {
            updateUserProfileImageUseCase(bytes, fileName, mimeType)
                .onSuccess { profile ->
                    editedProfileImageUrl = profile.profileImage
                    navigationManager.updateCurrentState { state ->
                        (state as? AdminSettingUiState.Setting)?.copy(profileImageUrl = profile.profileImage) ?: state
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
                                profileImageUrl = editedProfileImageUrl ?: userProfile.profileImage
                            )
                            ?: state
                    }
                }
            }

            getPlaceListUseCase()
                .onSuccess { places ->
                    // 장소 목록(#27)에는 관리자 프로필이 없어 place 카드에 아바타를 보여주려면(#217)
                    // 장소별로 관리자 목록을 추가 조회해야 한다. 장소 수만큼 한 번에 요청이 나가지
                    // 않도록 동시 조회 수를 제한한다.
                    val adminsSemaphore = Semaphore(MAX_CONCURRENT_DETAIL_REQUESTS)
                    val rooms = coroutineScope {
                        places.map { place ->
                            async {
                                val admins = adminsSemaphore.withPermit {
                                    getPlaceAdminsUseCase(place.placeId)
                                        .onFailure {
                                            println(
                                                "AdminSettingViewModel: 관리자 목록 조회 실패 - " +
                                                    "placeId=${place.placeId}, error=${it.message}"
                                            )
                                        }
                                        .getOrNull()
                                }
                                AdminSettingUiState.RoomItem(
                                    // Route 인자(Route.Admin.InviteCode/AddDevice)가 Int라 기존 관례를 따라 Int로 보관
                                    placeId = place.placeId.toInt(),
                                    roomName = place.name,
                                    isOwner = place.isOwner,
                                    admins = admins.orEmpty().map {
                                        AdminSettingUiState.AdminProfile(it.userId, it.name, it.profileImage, it.isOwner)
                                    },
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

    // 방장이 아닌 관리자가 장소에서 스스로 빠진다 - 장소 자체는 유지되고 내 카드만 목록에서 사라진다.
    private fun leaveRoom(placeId: Int) {
        viewModelScope.launch {
            leavePlaceUseCase(placeId.toLong())
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

    // 방장 위임 - 성공하면 해당 room의 isOwner/admins를 로컬에서 바로 갱신해 재조회 없이 반영한다.
    private fun transferOwner(placeId: Int, newOwnerUserId: Long) {
        viewModelScope.launch {
            transferPlaceOwnerUseCase(placeId.toLong(), newOwnerUserId)
                .onSuccess {
                    navigationManager.updateCurrentState { state ->
                        (state as? AdminSettingUiState.Setting)
                            ?.copy(
                                rooms = state.rooms.map { room ->
                                    if (room.placeId != placeId) return@map room
                                    room.copy(
                                        isOwner = false,
                                        admins = room.admins.map { admin ->
                                            admin.copy(isOwner = admin.userId == newOwnerUserId)
                                        },
                                    )
                                },
                            )
                            ?: state
                    }
                    navigationManager.emitEvent(AdminSettingEvent.ShowSnackbar("방장을 위임했습니다."))
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
