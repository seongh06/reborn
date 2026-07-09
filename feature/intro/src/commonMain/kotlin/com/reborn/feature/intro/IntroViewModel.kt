package com.reborn.feature.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.domain.usecase.GenerateAdminCodeUseCase
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.core.domain.usecase.LoginUseCase
import com.reborn.core.domain.usecase.RedeemAdminCodeUseCase
import com.reborn.core.domain.usecase.RegisterPlaceUseCase
import com.reborn.core.domain.usecase.UpdateFcmTokenUseCase
import com.reborn.core.model.DomainException
import com.reborn.core.model.Login
import com.reborn.core.notification.getFcmToken
import com.reborn.feature.intro.model.IntroIntent
import com.reborn.feature.intro.model.IntroUiState
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class IntroEvent {
    data object NavigateToAdmin : IntroEvent()
    data object NavigateToAerometer : IntroEvent()
    data object PermissionGranted : IntroEvent()
    data object ExitIntro : IntroEvent()
    // isNewUser뿐 아니라 "소속 장소가 하나도 없는 기존 유저"도 true - 관리자 등록 절차를 거쳐야 하는지 여부
    data class LoginSuccess(val needsPlaceSetup: Boolean) : IntroEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : IntroEvent()
    data class PlaceRegistered(val placeId: Long) : IntroEvent()
    data class AdminCodeIssued(val code: String, val remainingSeconds: Int) : IntroEvent()
    data object InviteCodeVerified : IntroEvent()
    data object InviteCodeInvalid : IntroEvent()
}

class IntroViewModel(
    private val loginUseCase: LoginUseCase,
    private val updateFcmTokenUseCase: UpdateFcmTokenUseCase,
    private val registerPlaceUseCase: RegisterPlaceUseCase,
    private val generateAdminCodeUseCase: GenerateAdminCodeUseCase,
    private val redeemAdminCodeUseCase: RedeemAdminCodeUseCase,
    private val getPlaceListUseCase: GetPlaceListUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<IntroUiState>(IntroUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<IntroEvent>()
    val event = _event.asSharedFlow()

    private val backStack = mutableListOf<IntroUiState>()

    // 장소 등록(이름 입력 → 유형 선택)이 두 화면에 걸쳐 있어, 등록 API 호출 시점(유형 선택 완료)까지 이름을 들고 있어야 함
    private var pendingPlaceName: String = ""

    // Setting의 "관리자 초대"(기존 장소)는 route로 placeId를 직접 받으므로 이 값을 쓰지 않고,
    // 온보딩 흐름(방금 등록한 장소)에서 AdminCode 화면에 placeId를 넘겨주기 위한 용도로만 쓰인다.
    var registeredPlaceId: Long? = null
        private set

    fun onIntent(intent: IntroIntent){
        when(intent){
            is IntroIntent.LoadInitial -> checkInitialState(intent.skipToAdminModeSelect)
            is IntroIntent.NavigateToTerm -> navigateTo(IntroUiState.Term)
            is IntroIntent.NavigateToPermission -> navigateTo(IntroUiState.Permission)
            is IntroIntent.NavigateToModeSelect -> navigateTo(IntroUiState.ModeSelect)
            is IntroIntent.NavigateToAdminLogin -> navigateTo(IntroUiState.AdminLogin)
            is IntroIntent.NavigateToAdminModeSelect -> navigateTo(IntroUiState.AdminModeSelect)
            is IntroIntent.NavigateToAdminPlaceName -> navigateTo(IntroUiState.AdminPlaceName)
            is IntroIntent.NavigateToAdminPlaceSelect -> navigateTo(IntroUiState.AdminPlaceSelect)
            is IntroIntent.NavigateToAerometerPairing -> navigateTo(IntroUiState.AerometerPairing)
            is IntroIntent.NavigateToInviteCode -> navigateTo(IntroUiState.InviteCode)
            is IntroIntent.NavigateToAdminCode -> navigateTo(IntroUiState.AdminCode)
            is IntroIntent.NavigateToAerometerDeviceName -> navigateTo(IntroUiState.AerometerDeviceName)
            is IntroIntent.NavigateBack -> navigateBack()
            is IntroIntent.PermissionsGranted -> onPermissionsGranted()
            is IntroIntent.NavigateToAdmin -> navigateToAdmin()
            is IntroIntent.NavigateToAerometer -> navigateToAerometer()
        }
    }

    private fun navigateTo(next: IntroUiState) {
        if (_uiState.value == next) return
        backStack.add(_uiState.value)
        _uiState.value = next
    }

    private fun navigateBack() {
        val previous = backStack.removeLastOrNull()
        if (previous != null) {
            _uiState.value = previous
        } else {
            // 더 돌아갈 내부 화면이 없으면 Intro 자체를 빠져나가는 건 호출부(outer onBackClick)에 맡긴다.
            viewModelScope.launch {
                _event.emit(IntroEvent.ExitIntro)
            }
        }
    }

    // skipToAdminModeSelect: Setting의 "새로운 place 추가"에서 진입할 때, Admin Login은 건너뛰고
    // 바로 그 다음 화면(AdminModeSelect)부터 보여주기 위한 플래그. 뒤로가기 시 backStack이 비어있어
    // ExitIntro가 곧바로 emit되므로, 건너뛴 화면들을 거치지 않고 호출부(Setting)로 바로 돌아감
    private fun checkInitialState(skipToAdminModeSelect: Boolean = false) {
        backStack.clear()
        if (skipToAdminModeSelect) {
            // 이미 로그인된 관리자가 재진입하는 경로이므로 Loading 스플래시 없이 바로 진입
            _uiState.value = IntroUiState.AdminModeSelect
            return
        }
        _uiState.value = IntroUiState.Loading
        viewModelScope.launch {
            delay(1500)
            _uiState.value = IntroUiState.Start
        }
    }

    private fun onPermissionsGranted() {
        viewModelScope.launch {
            _event.emit(IntroEvent.PermissionGranted)
        }
    }

    private fun navigateToAdmin() {
        viewModelScope.launch {
            _event.emit(IntroEvent.NavigateToAdmin)
        }
    }

    private fun navigateToAerometer() {
        viewModelScope.launch {
            _event.emit(IntroEvent.NavigateToAerometer)
        }
    }

    fun login(provider: String, token: String) {
        viewModelScope.launch {
            loginUseCase(Login(provider, token))
                .onSuccess { result ->
                    val needsPlaceSetup = resolveNeedsPlaceSetup(result.isNewUser)
                    _event.emit(IntroEvent.LoginSuccess(needsPlaceSetup))
                    // 기존 유저는 LoginSuccess 직후 네비게이션으로 IntroViewModel의 viewModelScope가
                    // 취소될 수 있어(#103 CodeRabbit 리뷰), FCM 등록만은 취소되지 않도록 보호한다.
                    withContext(NonCancellable) {
                        registerFcmToken()
                    }
                }
                .onFailure {
                    println("IntroViewModel: 로그인 API 실패 - provider=$provider, error=${it.message}")
                    _event.emit(IntroEvent.ShowErrorSnackbar(it))
                }
        }
    }

    // 신규 유저는 정의상 장소가 없으니 API 호출 없이 바로 등록 절차로 보낸다.
    // 기존 유저는 모든 장소에서 나가는 등으로 소속 장소가 0개일 수 있어 실제 목록을 조회해서 판단한다(#108).
    private suspend fun resolveNeedsPlaceSetup(isNewUser: Boolean): Boolean {
        if (isNewUser) return true
        return getPlaceListUseCase().fold(
            onSuccess = { places -> places.isEmpty() },
            // 조회 실패 시, 장소 없는 유저가 빈 홈 화면을 보게 되는 것보다는 등록 절차로 보내는 쪽이 안전하다.
            onFailure = {
                println("IntroViewModel: 장소 목록 조회 실패 - ${it.message}")
                true
            },
        )
    }

    // 로그인 성공 직후(accessToken 확보 이후)에만 서버로 FCM 토큰을 보낼 수 있어 이 시점에 호출.
    // 실패해도 로그인 플로우 자체를 막지 않는 best-effort 동작.
    private suspend fun registerFcmToken() {
        val token = runCatching { getFcmToken() }.getOrNull() ?: return
        updateFcmTokenUseCase(token)
            .onFailure { println("IntroViewModel: FCM 토큰 등록 실패 - ${it.message}") }
    }

    fun reportError(throwable: Throwable) {
        viewModelScope.launch {
            _event.emit(IntroEvent.ShowErrorSnackbar(throwable))
        }
    }

    fun setPlaceName(name: String) {
        pendingPlaceName = name
    }

    fun registerPlace(type: String) {
        viewModelScope.launch {
            registerPlaceUseCase(pendingPlaceName, type)
                .onSuccess { place ->
                    registeredPlaceId = place.placeId
                    _event.emit(IntroEvent.PlaceRegistered(place.placeId))
                }
                .onFailure {
                    println("IntroViewModel: 장소 등록 실패 - ${it.message}")
                    _event.emit(IntroEvent.ShowErrorSnackbar(it))
                }
        }
    }

    // 관리자 초대 코드 생성/재발급 공용 - AdminCode 화면 진입 시와 "재발급" 클릭 시 모두 호출됨
    fun generateAdminCode(placeId: Long) {
        viewModelScope.launch {
            generateAdminCodeUseCase(placeId)
                .onSuccess { code ->
                    // 서버 응답의 expiresAt(LocalDateTime 문자열)을 파싱할 날짜 라이브러리가 없어,
                    // 발급 직후 시점이라는 전제로 서버 TTL 상수를 그대로 남은 시간으로 사용한다.
                    _event.emit(IntroEvent.AdminCodeIssued(code.code, ADMIN_CODE_TTL_SECONDS))
                }
                .onFailure {
                    println("IntroViewModel: 관리자 코드 생성 실패 - ${it.message}")
                    _event.emit(IntroEvent.ShowErrorSnackbar(it))
                }
        }
    }

    fun verifyInviteCode(code: String) {
        viewModelScope.launch {
            redeemAdminCodeUseCase(code)
                .onSuccess { _event.emit(IntroEvent.InviteCodeVerified) }
                .onFailure {
                    println("IntroViewModel: 초대 코드 검증 실패 - ${it.message}")
                    // 코드 자체가 잘못됐거나 만료된 경우(400)만 인라인 에러로 처리하고,
                    // 그 외(네트워크/인증/서버 오류 등)는 registerPlace/generateAdminCode와 동일하게 스낵바로 알린다.
                    if (it is DomainException.InvalidInputException) {
                        _event.emit(IntroEvent.InviteCodeInvalid)
                    } else {
                        _event.emit(IntroEvent.ShowErrorSnackbar(it))
                    }
                }
        }
    }

    companion object {
        // 서버 PlaceService.ADMIN_INVITE_TTL_MINUTES(30분)와 동일
        const val ADMIN_CODE_TTL_SECONDS = 30 * 60
    }
}
