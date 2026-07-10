package com.reborn.feature.admin.feedback.model

import androidx.compose.runtime.Immutable
import com.reborn.core.ui.component.FeedbackType
import com.reborn.core.ui.component.State

@Immutable
sealed interface AdminFeedbackUiState {
    data object Loading : AdminFeedbackUiState
    data class Feedback(
        val feedbacks: List<FeedbackItem> = emptyList(),
        val feedbackFiltering: FeedbackFiltering = FeedbackFiltering.ALL
    ) : AdminFeedbackUiState

    data class FeedbackDetail(
        val feedbackId: Int,
        val feedback: FeedbackItem
    ) : AdminFeedbackUiState

    data class FeedbackQR(val placeId: Int) : AdminFeedbackUiState
    data class FeedbackItem(
        val id: Int,
        val type: FeedbackType,
        val state: State,
        val title: String,
        val time: String,
        val content: String
    )
    enum class FeedbackFiltering(val filtering: String, val state: State? = null) {
        ALL("전체", null),
        WAITING("대기", State.WAITING),
        APPROVE("승인", State.APPROVE),
        REJECT("거절", State.REJECT)
    }
}

fun AdminFeedbackUiState.Feedback.filteredFeedbacks(): List<AdminFeedbackUiState.FeedbackItem> {
    val targetState = feedbackFiltering.state ?: return feedbacks
    return feedbacks.filter { it.state == targetState }
}

sealed interface AdminFeedbackIntent{
    data object LoadInitial : AdminFeedbackIntent
    data object NavigateBack : AdminFeedbackIntent
    data class NavigateToQR(val placeId: Int) : AdminFeedbackIntent
    data class NavigateToFeedbackDetail(val feedbackId : Int) : AdminFeedbackIntent
    data class ClickTab(val tab: AdminFeedbackUiState.FeedbackFiltering) : AdminFeedbackIntent
    data class ApproveFeedback(val feedbackId: Int) : AdminFeedbackIntent
    data class RejectFeedback(val feedbackId: Int) : AdminFeedbackIntent
}