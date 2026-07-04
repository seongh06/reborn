package com.reborn.feature.admin.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.ui.component.FeedbackType
import com.reborn.core.ui.component.State
import com.reborn.feature.admin.feedback.model.AdminFeedbackIntent
import com.reborn.feature.admin.feedback.model.AdminFeedbackUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


sealed class AdminFeedbackEvent {
    data object Exit : AdminFeedbackEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminFeedbackEvent()
}

class AdminFeedbackViewModel : ViewModel() {
    private val navController = NavigationManager<AdminFeedbackUiState, AdminFeedbackEvent>(
        initialState = AdminFeedbackUiState.Loading,
        exitEvent = AdminFeedbackEvent.Exit,
        scope = viewModelScope
    )

    val uiState = navController.uiState
    val event = navController.event

    // TODO: 서버 feedback API 연동 전까지의 목업 데이터. 실제 연동 시 UseCase로 대체 예정
    private var feedbacks: List<AdminFeedbackUiState.FeedbackItem> = listOf(
        AdminFeedbackUiState.FeedbackItem(1, FeedbackType.HOT, State.WAITING, "너무 더워요", "5분전", "너무 더운데여. 배도 고파요."),
        AdminFeedbackUiState.FeedbackItem(2, FeedbackType.LIGHT, State.APPROVE, "불이 너무 밝아요", "10분전", "불이 너무 밝아서 눈이 아파요."),
        AdminFeedbackUiState.FeedbackItem(3, FeedbackType.AIR, State.REJECT, "공기가 안 좋아요", "1시간전", "공기청정기 좀 틀어주세요."),
        AdminFeedbackUiState.FeedbackItem(4, FeedbackType.COLD, State.WAITING, "너무 추워요", "2시간전", "난방 좀 틀어주세요."),
        AdminFeedbackUiState.FeedbackItem(5, FeedbackType.NOISE, State.APPROVE, "너무 시끄러워요", "어제", "밖에서 소리가 너무 크게 들려요."),
    )

    fun onIntent(intent: AdminFeedbackIntent) {
        when (intent) {
            is AdminFeedbackIntent.LoadInitial -> checkInitialState()
            is AdminFeedbackIntent.NavigateBack -> navController.navigateBack()
            is AdminFeedbackIntent.NavigateToFeedbackDetail -> navigateToFeedbackDetail(intent)
            is AdminFeedbackIntent.NavigateToQR -> navController.navigateTo(AdminFeedbackUiState.FeedbackQR(intent.placeId))
            is AdminFeedbackIntent.ClickTab -> handleTabClick(intent.tab)
        }
    }

    private fun checkInitialState() {
        navController.clearAndReset(AdminFeedbackUiState.Loading)
        viewModelScope.launch {
            delay(1500)
            navController.clearAndReset(AdminFeedbackUiState.Feedback(feedbacks))
        }
    }

    private fun navigateToFeedbackDetail(intent: AdminFeedbackIntent.NavigateToFeedbackDetail) {
        val feedback = feedbacks.find { it.id == intent.feedbackId } ?: return
        navController.navigateTo(AdminFeedbackUiState.FeedbackDetail(intent.feedbackId, feedback,))
    }


    fun handleTabClick(tab: AdminFeedbackUiState.FeedbackFiltering) {
        navController.updateCurrentState { state ->
            if (state is AdminFeedbackUiState.Feedback) {
                state.copy(feedbackFiltering = tab)
            } else state
        }
        loadData(tab)
    }

    private fun loadData(
        tab: AdminFeedbackUiState.FeedbackFiltering?=null
    ){

    }

}