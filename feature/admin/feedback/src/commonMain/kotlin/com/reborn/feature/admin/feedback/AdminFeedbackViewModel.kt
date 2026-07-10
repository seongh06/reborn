package com.reborn.feature.admin.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.domain.usecase.GetFeedbackListParams
import com.reborn.core.domain.usecase.GetFeedbackListUseCase
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.core.domain.usecase.UpdateFeedbackStatusUseCase
import com.reborn.core.model.Feedback
import com.reborn.core.ui.component.FeedbackType
import com.reborn.core.ui.component.State
import com.reborn.feature.admin.feedback.model.AdminFeedbackIntent
import com.reborn.feature.admin.feedback.model.AdminFeedbackUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


sealed class AdminFeedbackEvent {
    data object Exit : AdminFeedbackEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminFeedbackEvent()
}

// 서버 FeedbackItem에는 type(HOT/LIGHT/AIR 등) 분류 필드가 없어 항상 AIR로 고정.
// TODO: 백엔드에 피드백 분류 필드가 추가되면 실제 값으로 매핑
private fun Feedback.toUiItem(): AdminFeedbackUiState.FeedbackItem =
    AdminFeedbackUiState.FeedbackItem(
        id = feedbackId.toInt(),
        type = FeedbackType.AIR,
        state = status.toUiState(),
        title = content,
        time = createdAt.replace("T", " ").take(16),
        content = content,
    )

private fun String.toUiState(): State = when (this) {
    "APPROVED" -> State.APPROVE
    "REJECTED" -> State.REJECT
    else -> State.WAITING
}

private fun State.toServerStatus(): String = when (this) {
    State.APPROVE -> "APPROVED"
    State.REJECT -> "REJECTED"
    State.WAITING -> "PENDING"
}

class AdminFeedbackViewModel(
    private val getPlaceListUseCase: GetPlaceListUseCase,
    private val getFeedbackListUseCase: GetFeedbackListUseCase,
    private val updateFeedbackStatusUseCase: UpdateFeedbackStatusUseCase,
) : ViewModel() {
    private val navigationManager = NavigationManager<AdminFeedbackUiState, AdminFeedbackEvent>(
        initialState = AdminFeedbackUiState.Loading,
        exitEvent = AdminFeedbackEvent.Exit,
        scope = viewModelScope
    )

    val uiState = navigationManager.uiState
    val event = navigationManager.event

    // 관리자 앱이 여러 장소를 오가는 UI가 아직 없어 첫 장소로 임시 고정(#125, home/data와 동일 정책)
    private var placeId: Long? = null
    private var loadJob: Job? = null

    fun onIntent(intent: AdminFeedbackIntent) {
        when (intent) {
            is AdminFeedbackIntent.LoadInitial -> checkInitialState()
            is AdminFeedbackIntent.NavigateBack -> navigationManager.navigateBack()
            is AdminFeedbackIntent.NavigateToFeedbackDetail -> navigateToFeedbackDetail(intent)
            is AdminFeedbackIntent.NavigateToQR -> navigationManager.navigateTo(AdminFeedbackUiState.FeedbackQR(intent.placeId))
            is AdminFeedbackIntent.ClickTab -> handleTabClick(intent.tab)
            is AdminFeedbackIntent.ApproveFeedback -> updateStatus(intent.feedbackId, State.APPROVE)
            is AdminFeedbackIntent.RejectFeedback -> updateStatus(intent.feedbackId, State.REJECT)
        }
    }

    private fun checkInitialState() {
        navigationManager.clearAndReset(AdminFeedbackUiState.Loading)
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                refreshFeedbackList()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                navigationManager.emitEvent(AdminFeedbackEvent.ShowErrorSnackbar(e))
            }
        }
    }

    private suspend fun refreshFeedbackList() {
        val place = getPlaceListUseCase().getOrNull()?.firstOrNull()
        if (place == null) {
            navigationManager.clearAndReset(AdminFeedbackUiState.Feedback())
            return
        }
        placeId = place.placeId
        val feedbacks = getFeedbackListUseCase(GetFeedbackListParams(place.placeId))
            .getOrElse {
                navigationManager.emitEvent(AdminFeedbackEvent.ShowErrorSnackbar(it))
                emptyList()
            }
        navigationManager.clearAndReset(
            AdminFeedbackUiState.Feedback(feedbacks = feedbacks.map { it.toUiItem() })
        )
    }

    private fun navigateToFeedbackDetail(intent: AdminFeedbackIntent.NavigateToFeedbackDetail) {
        val state = navigationManager.uiState.value as? AdminFeedbackUiState.Feedback ?: return
        val feedback = state.feedbacks.find { it.id == intent.feedbackId }
            ?: return navigationManager.emitEvent(
                AdminFeedbackEvent.ShowErrorSnackbar(
                    IllegalArgumentException("피드백을 찾을 수 없습니다.")
                )
            )
        navigationManager.navigateTo(AdminFeedbackUiState.FeedbackDetail(intent.feedbackId, feedback))
    }


    private fun handleTabClick(tab: AdminFeedbackUiState.FeedbackFiltering) {
        navigationManager.updateCurrentState { state ->
            if (state is AdminFeedbackUiState.Feedback) {
                state.copy(feedbackFiltering = tab)
            } else state
        }
    }

    // 성공 시 목록을 다시 불러와 상세 화면에서 곧바로 목록으로 돌아간다 - NavigationManager의 backStack은
    // 이전 상태의 스냅샷이라 updateCurrentState로 상세 화면만 바꿔도 뒤로가기 시 목록은 갱신 전 값으로
    // 되돌아가므로, 아예 목록을 다시 불러와 clearAndReset하는 편이 상태 불일치가 없다
    private fun updateStatus(feedbackId: Int, newState: State) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                updateFeedbackStatusUseCase(feedbackId.toLong(), newState.toServerStatus()).getOrThrow()
                refreshFeedbackList()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                navigationManager.emitEvent(AdminFeedbackEvent.ShowErrorSnackbar(e))
            }
        }
    }

}
