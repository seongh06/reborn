package com.reborn.feature.admin.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.domain.usecase.GetPlaceDetailUseCase
import com.reborn.core.network.AppConfig
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

class AdminFeedbackViewModel(
    private val getPlaceDetailUseCase: GetPlaceDetailUseCase
) : ViewModel() {
    private val navigationManager = NavigationManager<AdminFeedbackUiState, AdminFeedbackEvent>(
        initialState = AdminFeedbackUiState.Loading,
        exitEvent = AdminFeedbackEvent.Exit,
        scope = viewModelScope
    )

    val uiState = navigationManager.uiState
    val event = navigationManager.event

    // TODO: 서버 feedback API 연동 전까지의 목업 데이터. 실제 연동 시 UseCase로 대체 예정
    private var feedbacks: List<AdminFeedbackUiState.FeedbackItem> = listOf(
        AdminFeedbackUiState.FeedbackItem(
            id = 1,
            type = FeedbackType.HOT,
            state = State.WAITING,
            title = "너무 더워요",
            time = "5분전",
            submittedAt = "2026.07.27 14:20",
            content = "너무 더운데여. 배도 고파요.",
            sensorSnapshot = AdminFeedbackUiState.SensorSnapshot(28.4, 55.0, 600, 5),
            temperatureAdjustment = AdminFeedbackUiState.TemperatureAdjustment(before = 26.0, after = 24.0)
        ),
        AdminFeedbackUiState.FeedbackItem(
            id = 2,
            type = FeedbackType.LIGHT,
            state = State.APPROVE,
            title = "불이 너무 밝아요",
            time = "10분전",
            submittedAt = "2026.07.27 14:15",
            content = "불이 너무 밝아서 눈이 아파요.",
            sensorSnapshot = AdminFeedbackUiState.SensorSnapshot(24.1, 48.0, 850, 3),
            temperatureAdjustment = AdminFeedbackUiState.TemperatureAdjustment(before = 24.0, after = 24.0)
        ),
        AdminFeedbackUiState.FeedbackItem(
            id = 3,
            type = FeedbackType.AIR,
            state = State.REJECT,
            title = "공기가 안 좋아요",
            time = "1시간전",
            submittedAt = "2026.07.27 13:25",
            content = "공기청정기 좀 틀어주세요.",
            sensorSnapshot = AdminFeedbackUiState.SensorSnapshot(23.5, 60.0, 400, 6),
            temperatureAdjustment = AdminFeedbackUiState.TemperatureAdjustment(before = 23.5, after = 23.5)
        ),
        AdminFeedbackUiState.FeedbackItem(
            id = 4,
            type = FeedbackType.COLD,
            state = State.WAITING,
            title = "너무 추워요",
            time = "2시간전",
            submittedAt = "2026.07.27 12:30",
            content = "난방 좀 틀어주세요.",
            sensorSnapshot = AdminFeedbackUiState.SensorSnapshot(19.2, 40.0, 300, 2),
            temperatureAdjustment = AdminFeedbackUiState.TemperatureAdjustment(before = 19.0, after = 22.0)
        ),
        AdminFeedbackUiState.FeedbackItem(
            id = 5,
            type = FeedbackType.NOISE,
            state = State.APPROVE,
            title = "너무 시끄러워요",
            time = "어제",
            submittedAt = "2026.07.26 21:40",
            content = "밖에서 소리가 너무 크게 들려요.",
            sensorSnapshot = AdminFeedbackUiState.SensorSnapshot(22.0, 50.0, 100, 4),
            temperatureAdjustment = AdminFeedbackUiState.TemperatureAdjustment(before = 22.0, after = 22.0)
        ),
    )

    fun onIntent(intent: AdminFeedbackIntent) {
        when (intent) {
            is AdminFeedbackIntent.LoadInitial -> checkInitialState(intent.feedbackId)
            is AdminFeedbackIntent.NavigateBack -> navigationManager.navigateBack()
            is AdminFeedbackIntent.NavigateToFeedbackDetail -> navigateToFeedbackDetail(intent)
            is AdminFeedbackIntent.NavigateToQR -> navigateToQR(intent.placeId)
            is AdminFeedbackIntent.ClickTab -> handleTabClick(intent.tab)
        }
    }

    private fun checkInitialState(feedbackId: Int? = null) {
        navigationManager.clearAndReset(AdminFeedbackUiState.Loading)
        viewModelScope.launch {
            delay(1500)
            navigationManager.clearAndReset(AdminFeedbackUiState.Feedback(feedbacks))
            if (feedbackId != null) {
                navigateToFeedbackDetail(AdminFeedbackIntent.NavigateToFeedbackDetail(feedbackId))
            }
        }
    }

    // QR 화면으로 즉시 전환(로딩 표시)한 뒤, 장소의 qrCode를 조회해서 방문자용 피드백 웹페이지
    // URL을 완성한다 - QR 이미지는 별도 라이브러리 없이 공개 QR 생성 API로 렌더링(#163)
    private fun navigateToQR(placeId: Int) {
        navigationManager.navigateTo(AdminFeedbackUiState.FeedbackQR(placeId))
        viewModelScope.launch {
            getPlaceDetailUseCase(placeId.toLong())
                .onSuccess { detail ->
                    val url = "${AppConfig.webBaseUrl}/feedback.html?qrCode=${detail.qrCode}"
                    navigationManager.updateCurrentState { state ->
                        (state as? AdminFeedbackUiState.FeedbackQR)?.copy(qrUrl = url) ?: state
                    }
                }
                .onFailure {
                    navigationManager.emitEvent(AdminFeedbackEvent.ShowErrorSnackbar(it))
                    navigationManager.updateCurrentState { state ->
                        (state as? AdminFeedbackUiState.FeedbackQR)?.copy(failed = true) ?: state
                    }
                }
        }
    }

    private fun navigateToFeedbackDetail(intent: AdminFeedbackIntent.NavigateToFeedbackDetail) {
        val feedback = feedbacks.find { it.id == intent.feedbackId }
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
        loadData(tab)
    }

    private fun loadData(
        tab: AdminFeedbackUiState.FeedbackFiltering?=null
    ){

    }

}