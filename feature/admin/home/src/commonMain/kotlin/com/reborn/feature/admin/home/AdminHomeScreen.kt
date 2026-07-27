package com.reborn.feature.admin.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.component.Dashboard
import com.reborn.core.ui.component.FeedbackStatusSection
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.home.component.FeedbackListSection
import com.reborn.feature.admin.home.component.IoTListSection
import com.reborn.feature.admin.home.model.AdminHomeIntent
import com.reborn.feature.admin.home.model.AdminHomeUiState
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminHomeRoute(
    viewModel: AdminHomeViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    navigateToFeedbackDetail: (Int) -> Unit,
    onNavigateToFeedbackList: () -> Unit = {},
    onNavigateToSetting: () -> Unit = {},
    onNavigateToDeviceList: () -> Unit = {},
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
                is AdminHomeEvent.NavigateToFeedbackList -> onNavigateToFeedbackList()
                is AdminHomeEvent.NavigateToSetting -> onNavigateToSetting()
                is AdminHomeEvent.NavigateToDeviceList -> onNavigateToDeviceList()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState)}
    ){_ ->
        when(val state = uiState) {
            is AdminHomeUiState.Loading -> RebornLoadingScreen()
            is AdminHomeUiState.Home -> AdminHomeScreen(
                onAlarmClick = {viewModel.onIntent(AdminHomeIntent.NavigateToAlarm)},
                onSettingClick = {viewModel.onIntent(AdminHomeIntent.NavigateToSetting)},
                onFeedbackClick = { id -> viewModel.onIntent(AdminHomeIntent.NavigateToFeedback(id)) },
                onMoreFeedbackClick = { viewModel.onIntent(AdminHomeIntent.NavigateToFeedbackList) },
                onDeviceListClick = { viewModel.onIntent(AdminHomeIntent.NavigateToDeviceList) }
            )
            is AdminHomeUiState.Alarm -> AdminAlarmScreen(
                state = state,
                onBackClick = { viewModel.onIntent(AdminHomeIntent.NavigateBack) },
                onFilterClick = { filter -> viewModel.onIntent(AdminHomeIntent.ClickAlarmFilter(filter)) },
                onAlarmDelete = { id -> viewModel.onIntent(AdminHomeIntent.DeleteAlarm(id)) }
            )
        }
    }
}

@Composable
fun AdminHomeScreen(
    onAlarmClick: () -> Unit,
    onSettingClick: () -> Unit,
    onFeedbackClick: (Int) -> Unit,
    onMoreFeedbackClick: () -> Unit = {},
    onDeviceListClick: () -> Unit = {},
    // TODO: 서버 device API 연동 전까지의 임시 플래그. 실제로는 device 목록 상태(null/empty)로 대체 예정
    hasDevices: Boolean = true
) {
    if (!hasDevices) {
         Column(
             modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
         ) {
             RebornTopAppBar(
                 title = "Re:Born",
                 onNavigateAlert = onAlarmClick,
                 onNavigateSetting = onSettingClick,
                 backgroundColor = RebornTheme.color.grayScale100
             )
             Column(
                 modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
                 horizontalAlignment = Alignment.CenterHorizontally,
                 verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
             ){
                 Icon(
                     painter = painterResource(Res.drawable.ic_none_iot),
                     contentDescription = null,
                     tint = RebornTheme.color.grayScale700,
                     modifier = Modifier.size(100.dp)
                 )
                 Text(
                     "현재 연결된 IoT 디바이스가 없어요\nIoT 디바이스를 추가해보세요",
                     style = RebornTheme.typography.bodyLarge,
                     color = RebornTheme.color.grayScale900,
                     textAlign = TextAlign.Center
                 )
             }
         }
    } else {
        Column(
            modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
        ) {
            RebornTopAppBar(
                title = "Re:Born",
                onNavigateAlert = onAlarmClick,
                onNavigateSetting = onSettingClick,
                backgroundColor = RebornTheme.color.grayScale100
            )
            // 바텀네비 캡슐 영역(상단 8dp + 캡슐 60dp + 하단 20dp = 88dp) 아래로 마지막
            // 아이템이 가려지지 않도록 하단 여백 확보. edge-to-edge라 콘텐츠는 그 영역까지
            // 실제로 그려지고, 스크롤 시 캡슐 위 그라데이션 스크림 너머로 비쳐 보임
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                item {
                    Dashboard(
                        temperature = /*state.metric?.temperature*/ 24.5f,
                        humidity = /*state.metric?.humidity*/ 48.5f,
                        illuminance = /*state.metric?.illuminance*/ 350f,
                        peopleCount = /*state.metric?.peopleCount*/ 3f
                    )
                }
                item {
                    FeedbackStatusSection(
                        modifier = Modifier.padding(16.dp, 8.dp),
                        totalCount = /*state.feedbacks.size*/15,
                        waitingCount = /*state.feedbacks.count { it.state == State.WAITING }*/ 3
                    )
                }
                item {
                    FeedbackListSection(
                        onFeedbackClick = onFeedbackClick,
                        onMoreClick = onMoreFeedbackClick
                    )
                }
                item {
                    IoTListSection(
                        onDeviceClick = { onDeviceListClick() },
                        onMoreClick = onDeviceListClick
                    )
                }
            }
        }
    }
}
