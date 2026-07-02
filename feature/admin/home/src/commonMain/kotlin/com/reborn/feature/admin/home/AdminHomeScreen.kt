package com.reborn.feature.admin.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.component.Dashboard
import com.reborn.core.ui.component.FeedbackItem
import com.reborn.core.ui.component.FeedbackType
import com.reborn.core.ui.component.State
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.home.model.AdminHomeIntent
import com.reborn.feature.admin.home.model.AdminHomeUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminHomeRoute(
    viewModel: AdminHomeViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    navigateToFeedbackDetail: (Int) -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {}
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        onBottomBarVisibilityChange(uiState !is AdminHomeUiState.Alarm)
    }

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
                is AdminHomeEvent.NavigateToFeedbackDetail -> navigateToFeedbackDetail(event.feedbackId)
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
                onSettingClick = {viewModel.onIntent(AdminHomeIntent.NavigateToSetting)},
                onFeedbackClick = {viewModel.onIntent(AdminHomeIntent.NavigateToFeedback(1))}
            )
            is AdminHomeUiState.Alarm -> AdminAlarmScreen(
                state = uiState,
                onBackClick = { viewModel.onIntent(AdminHomeIntent.NavigateBack) },
                onAlarmDelete = { id -> viewModel.onIntent(AdminHomeIntent.DeleteAlarm(id)) },
                onAlarmAllDelete = { viewModel.onIntent(AdminHomeIntent.DeleteAllAlarms) }
            )
            is AdminHomeUiState.Setting -> {}
        }
    }
}

@Composable
fun AdminHomeScreen(
    onAlarmClick: () -> Unit,
    onSettingClick: () -> Unit,
    viewModel: AdminHomeViewModel = koinViewModel(),
    onFeedbackClick: () -> Unit
) {
    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ){
        RebornTopAppBar(title = "HOME", onNavigateAlert = onAlarmClick, onNavigateSetting = onSettingClick)
        Dashboard("거실",20,20,20,20)
        Text("실시간 피드백",modifier = Modifier.padding(16.dp), style = RebornTheme.typography.titleSmall, color = RebornTheme.color.grayScale900)
        Column(
            modifier = Modifier.padding(16.dp,12.dp)
        ){
            FeedbackItem(
                id = 1,
                state = State.APPROVE,
                time = "5분전",
                title = "피드백 제목",
                type = FeedbackType.AIR,
                onClick = onFeedbackClick
            )
        }
    }
}
