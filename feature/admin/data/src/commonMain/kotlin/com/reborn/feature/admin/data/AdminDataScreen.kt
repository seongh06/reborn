package com.reborn.feature.admin.data

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.component.SelectOptionRow
import com.reborn.core.ui.component.TabBar
import com.reborn.core.ui.component.TutorialHighlightOverlay
import com.reborn.core.ui.component.tutorialTarget
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.data.component.section.AnalysisResultSection
import com.reborn.feature.admin.data.component.section.DataLineChartSection
import com.reborn.feature.admin.data.model.AdminDataIntent
import com.reborn.feature.admin.data.model.AdminDataUiState
import org.koin.compose.viewmodel.koinViewModel

// 최초 접속 튜토리얼(#240) 설명 문구 - 바텀 네비 자리에 대신 뜨는 TutorialHintCard(App.kt)에서 쓴다.
private const val REPORT_TUTORIAL_HINT =
    "여기서 온습도 등 데이터 분석 결과를 확인할 수 있어요. AI가 알아서 요약해줘요!"

@Composable
fun AdminDataRoute(
    viewModel: AdminDataViewModel = koinViewModel(),
    onBackClick: () -> Unit = {},
    onTutorialHintChange: (String?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    SideEffect {
        val state = uiState
        onTutorialHintChange(
            if (state is AdminDataUiState.Data && state.showReportHint) REPORT_TUTORIAL_HINT else null
        )
    }

    LaunchedEffect(Unit) {
        viewModel.onIntent(AdminDataIntent.LoadInitial)

        viewModel.event.collect { event ->
            when (event) {
                is AdminDataEvent.ShowErrorSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.throwable.message ?: "에러가 발생했습니다."
                    )
                }
                is AdminDataEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
                is AdminDataEvent.OpenUrl -> uriHandler.openUri(event.url)
                is AdminDataEvent.Exit -> onBackClick()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        when (val state = uiState) {
            is AdminDataUiState.Loading -> RebornLoadingScreen()
            is AdminDataUiState.Data -> AdminDataScreen(
                state = state,
                onCategoryClick = { category -> viewModel.onIntent(AdminDataIntent.ClickCategoryTab(category)) },
                onPeriodClick = { period -> viewModel.onIntent(AdminDataIntent.ClickPeriod(period)) },
                onExportClick = { viewModel.onIntent(AdminDataIntent.ClickExport) },
                onDismissTutorial = { viewModel.onIntent(AdminDataIntent.DismissTutorial) }
            )
        }
    }
}

@Composable
fun AdminDataScreen(
    state: AdminDataUiState.Data,
    onCategoryClick: (AdminDataUiState.Category) -> Unit,
    onPeriodClick: (AdminDataUiState.Period) -> Unit,
    onExportClick: () -> Unit,
    onDismissTutorial: () -> Unit = {}
) {
    var reportSectionRect by remember { mutableStateOf<Rect?>(null) }

    Box {
        Column(
            modifier = Modifier
                .rebornDefault(Color.White)
                .verticalScroll(rememberScrollState())
        ) {
            RebornTopAppBar(title = "${state.place} 보고서", onNavigateDataExport = onExportClick)
            TabBar(
                tabItems = state.availableCategories,
                selectedTab = state.selectedCategory,
                onTabSelected = onCategoryClick,
                getDisplayName = { it.label }
            )
            Column(
            ) {
                DataLineChartSection(
                    labels = state.chartLabels,
                    values = state.chartValues,
                    hasData = state.hasEnoughData
                )
                SelectOptionRow(
                    modifier = Modifier.padding(16.dp, 12.dp),
                    options = AdminDataUiState.Period.entries,
                    selectedOption = state.selectedPeriod,
                    onOptionSelected = onPeriodClick,
                    optionToString = { it.label }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(RebornTheme.color.grayScale200)
                )
            }
            AnalysisResultSection(
                text = state.analysisText,
                modifier = Modifier.tutorialTarget { reportSectionRect = it }
            )
        }

        // 최초 접속 튜토리얼(#240) - "분석 결과" 섹션을 강조.
        if (state.showReportHint) {
            TutorialHighlightOverlay(
                highlightRect = reportSectionRect,
                onDismiss = onDismissTutorial
            )
        }
    }
}
