package com.reborn.feature.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.intro.model.IntroIntent
import com.reborn.feature.intro.model.IntroUiState
import com.reborn.feature.intro.screen.IntroModeSelectScreen
import com.reborn.feature.intro.screen.IntroPermissionScreen
import com.reborn.feature.intro.screen.IntroTermScreen
import com.reborn.feature.intro.screen.admin.IntroAdminCodeScreen
import com.reborn.feature.intro.screen.admin.IntroAdminLoginScreen
import com.reborn.feature.intro.screen.admin.IntroAdminModeSelectScreen
import com.reborn.feature.intro.screen.admin.IntroAdminPlaceNameScreen
import com.reborn.feature.intro.screen.admin.IntroAdminPlaceSelectScreen
import com.reborn.feature.intro.screen.admin.IntroInviteCodeScreen
import com.reborn.feature.intro.screen.aerometer.IntroAermeterPairingScreen
import com.reborn.feature.intro.screen.aerometer.IntroAerometerDeviceNameScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntroRoute(
    viewModel: IntroViewModel = koinViewModel(),
    onNavigateToAdmin: () -> Unit,
    onNavigateToAerometer: () -> Unit,
    onBackClick: () -> Unit,
    skipToAdminModeSelect: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onIntent(IntroIntent.LoadInitial(skipToAdminModeSelect = skipToAdminModeSelect))

        viewModel.event.collect { event ->
            when (event) {
                is IntroEvent.ShowErrorSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.throwable.message ?: "에러가 발생했습니다."
                    )
                }
                is IntroEvent.NavigateToAdmin -> onNavigateToAdmin()
                is IntroEvent.NavigateToAerometer -> onNavigateToAerometer()
                is IntroEvent.PermissionGranted -> {}
                is IntroEvent.ExitIntro -> onBackClick()
                is IntroEvent.LoginSuccess -> {} // IntroAdminLoginScreen에서 자체적으로 처리
                is IntroEvent.PlaceRegistered -> {} // IntroAdminPlaceSelectScreen에서 자체적으로 처리
                is IntroEvent.AdminCodeIssued -> {} // IntroAdminCodeScreen에서 자체적으로 처리
                is IntroEvent.InviteCodeVerified -> {} // IntroInviteCodeScreen에서 자체적으로 처리
                is IntroEvent.InviteCodeInvalid -> {} // IntroInviteCodeScreen에서 자체적으로 처리
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        when(uiState){
            is IntroUiState.Loading -> RebornLoadingScreen()
            is IntroUiState.Start -> IntroScreen(
                onNextClick = { viewModel.onIntent(IntroIntent.NavigateToTerm) }
            )
            is IntroUiState.Term -> IntroTermScreen(
                onNextClick = { viewModel.onIntent(IntroIntent.NavigateToPermission) },
                onBackClick = { viewModel.onIntent(IntroIntent.NavigateBack) }
            )
            is IntroUiState.Permission -> IntroPermissionScreen(
                onNextClick = { viewModel.onIntent(IntroIntent.NavigateToModeSelect) },
                onBackClick = { viewModel.onIntent(IntroIntent.NavigateBack) }
            )
            is IntroUiState.ModeSelect -> IntroModeSelectScreen(
                onAerometerClick = { viewModel.onIntent(IntroIntent.NavigateToAerometerPairing) },
                onAdminClick = { viewModel.onIntent(IntroIntent.NavigateToAdminLogin) },
                onBackClick = { viewModel.onIntent(IntroIntent.NavigateBack) }
            )
            is IntroUiState.AerometerPairing -> IntroAermeterPairingScreen(
                onPairingComplete = { viewModel.onIntent(IntroIntent.NavigateToAerometerDeviceName) },
                onBackClick = { viewModel.onIntent(IntroIntent.NavigateBack) }
            )
            is IntroUiState.AerometerDeviceName -> IntroAerometerDeviceNameScreen(
                onNextClick = { viewModel.onIntent(IntroIntent.NavigateToAerometer) },
                onBackClick = { viewModel.onIntent(IntroIntent.NavigateBack) }
            )
            is IntroUiState.AdminLogin -> IntroAdminLoginScreen(
                onLoginClick = { isNewUser ->
                    if (isNewUser) {
                        // 신규 유저: 장소 등록/페어링 등 초기 설정 플로우를 거쳐야 함
                        viewModel.onIntent(IntroIntent.NavigateToAdminModeSelect)
                    } else {
                        // 기존 유저: 초기 설정 없이 바로 메인 화면(AdminHomeScreen)으로 이동
                        viewModel.onIntent(IntroIntent.NavigateToAdmin)
                    }
                }
            )
            is IntroUiState.AdminModeSelect -> IntroAdminModeSelectScreen(
                onInviteCodeClick = { viewModel.onIntent(IntroIntent.NavigateToInviteCode) },
                onNewClick = { viewModel.onIntent(IntroIntent.NavigateToAdminPlaceName) }
            )
            is IntroUiState.InviteCode -> IntroInviteCodeScreen(
                onNextClick = { viewModel.onIntent(IntroIntent.NavigateToAdmin) },
                onBackClick = { viewModel.onIntent(IntroIntent.NavigateBack) }
            )
            is IntroUiState.AdminPlaceName -> IntroAdminPlaceNameScreen(
                onNextClick = { viewModel.onIntent(IntroIntent.NavigateToAdminPlaceSelect) },
                onBackClick = { viewModel.onIntent(IntroIntent.NavigateBack) }
            )
            is IntroUiState.AdminPlaceSelect -> IntroAdminPlaceSelectScreen(
                onNextClick = { viewModel.onIntent(IntroIntent.NavigateToAdminCode) },
                onBackClick = { viewModel.onIntent(IntroIntent.NavigateBack) }
            )
            is IntroUiState.AdminCode -> IntroAdminCodeScreen(
                placeId = viewModel.registeredPlaceId,
                onBackClick = { viewModel.onIntent(IntroIntent.NavigateBack) },
                onNextClick = { viewModel.onIntent(IntroIntent.NavigateToAdmin) }
            )
        }
    }
}

@Composable
fun IntroScreen(
    onNextClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        RebornButton(
            text = "시작하기",
            onClick = onNextClick
        )
    }
}
