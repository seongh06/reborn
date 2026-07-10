package com.reborn.feature.admin.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.component.FeedbackItem
import com.reborn.core.ui.component.State
import com.reborn.core.ui.component.TabBar
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.feedback.component.FeedbackStatusSection
import com.reborn.feature.admin.feedback.model.AdminFeedbackIntent
import com.reborn.feature.admin.feedback.model.AdminFeedbackUiState
import com.reborn.feature.admin.feedback.model.filteredFeedbacks
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun AdminFeedbackRoute(
    viewModel: AdminFeedbackViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {}
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(uiState) {
        onBottomBarVisibilityChange(
            uiState !is AdminFeedbackUiState.FeedbackQR && uiState !is AdminFeedbackUiState.FeedbackDetail
        )
    }

    LaunchedEffect(Unit) {
        viewModel.onIntent(AdminFeedbackIntent.LoadInitial)

        viewModel.event.collect { event ->
            when (event) {
                is AdminFeedbackEvent.ShowErrorSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.throwable.message ?: "에러가 발생했습니다."
                    )
                }
                is AdminFeedbackEvent.Exit -> onBackClick()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState)}
    ){_ ->
        when(val state = uiState) {
            is AdminFeedbackUiState.Loading -> RebornLoadingScreen()
            is AdminFeedbackUiState.Feedback -> AdminFeedbackScreen(
                state = state,
                navToFeedbackQR = { id -> viewModel.onIntent(AdminFeedbackIntent.NavigateToQR(id))},
                navToFeedbackDetail = { id -> viewModel.onIntent(AdminFeedbackIntent.NavigateToFeedbackDetail(id)) },
                onTabClick = { tab -> viewModel.onIntent(AdminFeedbackIntent.ClickTab(tab))}
            )
            is AdminFeedbackUiState.FeedbackQR -> AdminFeedbackQRScreen(
                state = state,
                onBackClick = onBackClick,
                onDownloadClick = {}
            )
            is AdminFeedbackUiState.FeedbackDetail -> AdminFeedbackDetailScreen(
                state = state,
                onBackClick = onBackClick,
                onRejectClick = { viewModel.onIntent(AdminFeedbackIntent.RejectFeedback(state.feedbackId)) },
                onApproveClick = { viewModel.onIntent(AdminFeedbackIntent.ApproveFeedback(state.feedbackId)) }
            )
        }
    }
}

@Composable
fun AdminFeedbackScreen(
    state: AdminFeedbackUiState.Feedback,
    onTabClick: (AdminFeedbackUiState.FeedbackFiltering) -> Unit = {},
    navToFeedbackQR: (Int) -> Unit,
    navToFeedbackDetail: (Int) -> Unit
) {

    val currentTab = state.feedbackFiltering
    val placeId = 1
    val filteredFeedbacks = state.filteredFeedbacks()

    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ) {
        RebornTopAppBar(title = "피드백", onNavigateFeedbackQR = { navToFeedbackQR(placeId) })
        FeedbackStatusSection(
            modifier = Modifier.padding(16.dp, 8.dp),
            totalCount = state.feedbacks.size,
            waitingCount = state.feedbacks.count { it.state == State.WAITING }
        )
        TabBar(
            tabItems = AdminFeedbackUiState.FeedbackFiltering.entries,
            selectedTab = currentTab,
            onTabSelected = onTabClick,
            getDisplayName = { it.filtering }
        )
        if (filteredFeedbacks.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "해당하는 피드백이 없습니다",
                    style = RebornTheme.typography.bodyMedium,
                    color = RebornTheme.color.grayScale500
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp, 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredFeedbacks.forEach { feedback ->
                    FeedbackItem(
                        id = feedback.id,
                        state = feedback.state,
                        time = feedback.time,
                        title = feedback.title,
                        type = feedback.type,
                        onClick = { navToFeedbackDetail(feedback.id) }
                    )
                }
            }
        }
    }
}
