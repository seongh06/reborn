package com.reborn.feature.admin.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.component.Dashboard
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.home.model.AdminHomeIntent
import com.reborn.feature.admin.home.model.AdminHomeUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminHomeRoute(
    viewModel: AdminHomeViewModel = koinViewModel(),
    onBackClick: () -> Unit
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onIntent(AdminHomeIntent.LoadInitial)

        viewModel.event.collect { event ->
            when (event) {
                is AdminHomeEvent.ShowErrorSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.throwable.message ?: "에러가 발생했습니다."
                    )
                }
                is AdminHomeEvent.Exit -> onBackClick()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState)}
    ){_ ->
        when(uiState) {
            is AdminHomeUiState.Loading -> RebornLoadingScreen()
            is AdminHomeUiState.Home -> AdminHomeScreen(
                onAlarmClick = {viewModel.onIntent(AdminHomeIntent.NavigateToAlarm)},
                onSettingClick = {viewModel.onIntent(AdminHomeIntent.NavigateToSetting)}
            )
            is AdminHomeUiState.Alarm -> {}
            is AdminHomeUiState.Setting -> {}
        }
    }
}

@Composable
fun AdminHomeScreen(
    onAlarmClick: () -> Unit,
    onSettingClick: () -> Unit,
    viewModel: AdminHomeViewModel = koinViewModel()
) {
    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ){
        RebornTopAppBar(title = "HOME", onNavigateAlert = onAlarmClick, onNavigateSetting = onSettingClick)
        Dashboard("거실",20,20,20,20)
    }
}
