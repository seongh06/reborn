package com.reborn.feature.admin.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.component.Dashboard
import com.reborn.core.ui.component.FeedbackStatusSection
import com.reborn.core.ui.component.TutorialHighlightOverlay
import com.reborn.core.ui.component.tutorialTarget
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.home.component.FeedbackListSection
import com.reborn.feature.admin.home.component.IoTListSection
import com.reborn.feature.admin.home.model.AdminHomeIntent
import com.reborn.feature.admin.home.model.AdminHomeUiState
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

// SmartThings 연결 하이라이트(#240 1단계)의 설명 문구 - 바텀 네비 자리에 대신 뜨는
// TutorialHintCard(App.kt)에서 쓴다.
private const val SMART_THINGS_TUTORIAL_HINT =
    "아직 연결된 기기가 없네요. SmartThings 계정을 연동하면 에어컨 등 기기를 바로 제어할 수 있어요. 여기를 눌러서 시작해보세요!"

@Composable
fun AdminHomeRoute(
    viewModel: AdminHomeViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    navigateToFeedbackDetail: (Int) -> Unit,
    onNavigateToFeedbackList: () -> Unit = {},
    onNavigateToSetting: () -> Unit = {},
    onNavigateToDeviceList: () -> Unit = {},
    onNavigateToDeviceDetail: (String) -> Unit = {},
    onNavigateToAddSmartThingsDevice: () -> Unit = {},
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
    onTutorialHintChange: (String?) -> Unit = {}
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // IoT 기기가 하나도 없으면 다른 탭(예: Data)으로 가도 보여줄 값이 없어서 바텀 네비 자체를
    // 숨긴다 - 튜토리얼 카드는 App.kt에서 이 값과 별개로(먼저) 처리되어 계속 뜬다.
    LaunchedEffect(uiState) {
        val visible = when (val state = uiState) {
            is AdminHomeUiState.Home -> state.hasDevices
            is AdminHomeUiState.Alarm -> false
            is AdminHomeUiState.Loading -> false
        }
        onBottomBarVisibilityChange(visible)
    }

    // 튜토리얼이 떠 있는 동안엔 바텀 네비 자리를 설명 카드가 대신한다(#240) - 화면이 바뀌거나
    // 튜토리얼이 꺼지면 null을 흘려보내 원래 바텀 네비로 되돌아가게 함.
    SideEffect {
        val state = uiState
        onTutorialHintChange(
            if (state is AdminHomeUiState.Home && state.showTutorialHint) SMART_THINGS_TUTORIAL_HINT else null
        )
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
                is AdminHomeEvent.NavigateToDeviceDetail -> onNavigateToDeviceDetail(event.deviceId)
                is AdminHomeEvent.NavigateToAddSmartThingsDevice -> onNavigateToAddSmartThingsDevice()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState)}
    ){_ ->
        when(val state = uiState) {
            is AdminHomeUiState.Loading -> RebornLoadingScreen()
            is AdminHomeUiState.Home -> AdminHomeScreen(
                state = state,
                onAlarmClick = {viewModel.onIntent(AdminHomeIntent.NavigateToAlarm)},
                onSettingClick = {viewModel.onIntent(AdminHomeIntent.NavigateToSetting)},
                onFeedbackClick = { id -> viewModel.onIntent(AdminHomeIntent.NavigateToFeedback(id)) },
                onMoreFeedbackClick = { viewModel.onIntent(AdminHomeIntent.NavigateToFeedbackList) },
                onDeviceListClick = { viewModel.onIntent(AdminHomeIntent.NavigateToDeviceList) },
                onDeviceDetailClick = { id -> viewModel.onIntent(AdminHomeIntent.NavigateToDeviceDetail(id)) },
                onDevicePowerToggle = { id -> viewModel.onIntent(AdminHomeIntent.TogglePower(id)) },
                onAddSmartThingsClick = { viewModel.onIntent(AdminHomeIntent.NavigateToAddSmartThingsDevice) },
                onDismissTutorial = { viewModel.onIntent(AdminHomeIntent.DismissTutorial) }
            )
            is AdminHomeUiState.Alarm -> AdminAlarmScreen(
                state = state,
                onBackClick = { viewModel.onIntent(AdminHomeIntent.NavigateBack) },
                onFilterClick = { filter -> viewModel.onIntent(AdminHomeIntent.ClickAlarmFilter(filter)) },
                onAlarmDelete = { id -> viewModel.onIntent(AdminHomeIntent.DeleteAlarm(id)) },
                onAlarmClick = { id -> viewModel.onIntent(AdminHomeIntent.NavigateToFeedback(id)) }
            )
        }
    }
}

@Composable
fun AdminHomeScreen(
    state: AdminHomeUiState.Home,
    onAlarmClick: () -> Unit,
    onSettingClick: () -> Unit,
    onFeedbackClick: (Int) -> Unit,
    onMoreFeedbackClick: () -> Unit = {},
    onDeviceListClick: () -> Unit = {},
    onDeviceDetailClick: (String) -> Unit = {},
    onDevicePowerToggle: (String) -> Unit = {},
    onAddSmartThingsClick: () -> Unit = {},
    onDismissTutorial: () -> Unit = {}
) {
    if (!state.hasDevices) {
         var smartThingsHintRect by remember { mutableStateOf<Rect?>(null) }

         Box {
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
                     // 빈 상태에서 실제로 기기를 추가할 방법이 없었던 문제(#217) - 작은 링크형 버튼으로
                     // SmartThings 연동(등록) 화면 진입점을 바로 제공한다.
                     Text(
                         "SmartThings 연결",
                         style = RebornTheme.typography.labelMedium,
                         color = RebornTheme.color.grayScale700,
                         textDecoration = TextDecoration.Underline,
                         modifier = Modifier
                             .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                             .clickable(
                                 role = Role.Button,
                                 onClickLabel = "SmartThings 연결",
                                 onClick = onAddSmartThingsClick
                             )
                             .padding(8.dp)
                             // 실제 눈에 보이는 텍스트+여백 전체(패딩 포함)를 그대로 하이라이트
                             // 영역으로 써야 잘려 보이지 않는다 - 체인 맨 끝(가장 바깥)에 둔다.
                             .tutorialTarget { smartThingsHintRect = it }
                     )
                 }
             }

             // 최초 접속 튜토리얼(#240) 1단계 - 신규 사용자에게 SmartThings 연결 진입점을 강조.
             if (state.showTutorialHint) {
                 TutorialHighlightOverlay(
                     highlightRect = smartThingsHintRect,
                     onDismiss = onDismissTutorial
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
                        temperature = state.metric?.temperature?.toFloat(),
                        humidity = state.metric?.humidity?.toFloat(),
                        illuminance = state.metric?.illuminance?.toFloat(),
                        peopleCount = state.metric?.peopleCount?.toFloat()
                    )
                }
                item {
                    FeedbackStatusSection(
                        modifier = Modifier.padding(16.dp, 8.dp),
                        totalCount = state.feedbackTotalCount,
                        waitingCount = state.feedbackWaitingCount
                    )
                }
                item {
                    FeedbackListSection(
                        recentFeedbacks = state.recentFeedbacks,
                        onFeedbackClick = onFeedbackClick,
                        onMoreClick = onMoreFeedbackClick
                    )
                }
                item {
                    IoTListSection(
                        devices = state.devices,
                        onDeviceClick = onDeviceDetailClick,
                        onPowerToggle = onDevicePowerToggle,
                        onMoreClick = onDeviceListClick
                    )
                }
            }
        }
    }
}
