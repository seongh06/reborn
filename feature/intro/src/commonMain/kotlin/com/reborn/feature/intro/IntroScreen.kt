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
import com.reborn.feature.intro.screen.IntroPermissionScreen
import com.reborn.feature.intro.screen.IntroTermScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntroRoute(
    viewModel: IntroViewModel = koinViewModel(),
    onNavigateToAdmin: () -> Unit,
    onNavigateToAerometer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onIntent(IntroIntent.LoadInitial)

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
                onBackClick = { viewModel.onIntent(IntroIntent.LoadInitial) }
            )
            is IntroUiState.Permission -> IntroPermissionScreen(
                onNextClick = { viewModel.onIntent(IntroIntent.NavigateToAdmin) },
                onBackClick = { viewModel.onIntent(IntroIntent.LoadInitial) }
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
