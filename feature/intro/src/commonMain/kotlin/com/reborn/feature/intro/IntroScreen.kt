package com.reborn.feature.intro

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.feature.intro.model.IntroIntent
import com.reborn.feature.intro.model.IntroUiState
import com.reborn.feature.intro.screen.IntroWelcomeScreen
import com.reborn.feature.intro.screen.admin.IntroDevicePairingCodeScreen
import com.reborn.feature.intro.screen.admin.IntroInviteCodeScreen
import com.reborn.feature.intro.screen.admin.IntroSignupScreen
import com.reborn.feature.intro.screen.aerometer.IntroAermeterPairingScreen
import com.reborn.feature.intro.screen.aerometer.IntroAerometerDeviceNameScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntroRoute(
    viewModel: IntroViewModel = koinViewModel(),
    onNavigateToAdmin: () -> Unit,
    onNavigateToAerometer: () -> Unit,
    onBackClick: () -> Unit,
    onTermsClick: (type: String) -> Unit = {},
    skipToSignup: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onIntent(IntroIntent.LoadInitial(skipToSignup = skipToSignup))

        viewModel.event.collect { event ->
            when (event) {
                is IntroEvent.ShowErrorSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.throwable.message ?: "에러가 발생했습니다."
                    )
                }
                is IntroEvent.NavigateToAdmin -> onNavigateToAdmin()
                is IntroEvent.NavigateToAerometer -> onNavigateToAerometer()
                is IntroEvent.ExitIntro -> onBackClick()
                is IntroEvent.LoginSuccess -> {} // IntroWelcomeScreen에서 자체적으로 처리
                is IntroEvent.PlaceRegistered -> {} // IntroSignupScreen에서 자체적으로 처리
                is IntroEvent.AdminCodeIssued -> {} // IntroAdminCodeScreen에서 자체적으로 처리
                is IntroEvent.PairingCodeIssued -> {} // IntroDevicePairingCodeScreen에서 자체적으로 처리
                is IntroEvent.InviteCodeVerified -> {} // IntroInviteCodeScreen에서 자체적으로 처리
                is IntroEvent.InviteCodeInvalid -> {} // IntroInviteCodeScreen에서 자체적으로 처리
                is IntroEvent.DevicePaired -> {} // IntroAerometerDeviceNameScreen에서 자체적으로 처리
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        when(uiState){
            is IntroUiState.Loading -> RebornLoadingScreen()
            is IntroUiState.Welcome -> IntroWelcomeScreen(
                onLoginSuccess = { needsPlaceSetup ->
                    if (needsPlaceSetup) {
                        // 신규 유저이거나, 기존 유저지만 소속 장소가 없는 경우(#108) — 장소 등록/페어링 등 초기 설정 플로우를 거쳐야 함
                        viewModel.onIntent(IntroIntent.NavigateToSignup)
                    } else {
                        // 소속 장소가 있는 기존 유저: 초기 설정 없이 바로 메인 화면(AdminHomeScreen)으로 이동
                        viewModel.onIntent(IntroIntent.NavigateToAdmin)
                    }
                },
                onAerometerClick = { viewModel.onIntent(IntroIntent.NavigateToAerometerPairing) },
                onTermsClick = onTermsClick
            )
            is IntroUiState.AerometerPairing -> IntroAermeterPairingScreen(
                onPairingComplete = { viewModel.onIntent(IntroIntent.NavigateToAerometerDeviceName) },
                onBackClick = { viewModel.onIntent(IntroIntent.NavigateBack) }
            )
            is IntroUiState.AerometerDeviceName -> IntroAerometerDeviceNameScreen(
                onNextClick = { viewModel.onIntent(IntroIntent.NavigateToAerometer) },
                onBackClick = { viewModel.onIntent(IntroIntent.NavigateBack) }
            )
            is IntroUiState.Signup -> IntroSignupScreen(
                onNextClick = { viewModel.onIntent(IntroIntent.NavigateToDevicePairing) },
                onBackClick = { viewModel.onIntent(IntroIntent.NavigateBack) },
                onInviteCodeClick = { viewModel.onIntent(IntroIntent.NavigateToInviteCode) }
            )
            is IntroUiState.InviteCode -> IntroInviteCodeScreen(
                onNextClick = { viewModel.onIntent(IntroIntent.NavigateToAdmin) },
                onBackClick = { viewModel.onIntent(IntroIntent.NavigateBack) }
            )
            is IntroUiState.DevicePairing -> IntroDevicePairingCodeScreen(
                placeId = viewModel.registeredPlaceId,
                onBackClick = { viewModel.onIntent(IntroIntent.NavigateBack) },
                onNextClick = { viewModel.onIntent(IntroIntent.NavigateToAdmin) }
            )
        }
    }
}
