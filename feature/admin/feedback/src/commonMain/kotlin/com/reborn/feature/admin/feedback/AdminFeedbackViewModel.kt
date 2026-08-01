package com.reborn.feature.admin.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.GalleryImageSaver
import com.reborn.core.common.NavigationManager
import com.reborn.core.domain.usecase.GetFeedbackListUseCase
import com.reborn.core.domain.usecase.GetPlaceDetailUseCase
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.core.domain.usecase.UpdateFeedbackStatusUseCase
import com.reborn.core.model.Feedback
import com.reborn.core.ui.component.State
import com.reborn.core.ui.component.classifyFeedbackType
import com.reborn.core.ui.component.feedbackStatusToState
import com.reborn.core.ui.component.formatFeedbackRelativeTime
import com.reborn.feature.admin.feedback.model.AdminFeedbackIntent
import com.reborn.feature.admin.feedback.model.AdminFeedbackUiState
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime


sealed class AdminFeedbackEvent {
    data object Exit : AdminFeedbackEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminFeedbackEvent()
    data class ShowSnackbar(val message: String) : AdminFeedbackEvent()
}

class AdminFeedbackViewModel(
    private val getPlaceListUseCase: GetPlaceListUseCase,
    private val getPlaceDetailUseCase: GetPlaceDetailUseCase,
    private val getFeedbackListUseCase: GetFeedbackListUseCase,
    private val updateFeedbackStatusUseCase: UpdateFeedbackStatusUseCase,
    private val galleryImageSaver: GalleryImageSaver,
) : ViewModel() {
    private val navigationManager = NavigationManager<AdminFeedbackUiState, AdminFeedbackEvent>(
        initialState = AdminFeedbackUiState.Loading,
        exitEvent = AdminFeedbackEvent.Exit,
        scope = viewModelScope
    )

    val uiState = navigationManager.uiState
    val event = navigationManager.event

    private var feedbacks: List<AdminFeedbackUiState.FeedbackItem> = emptyList()

    // TODO: 장소 선택/전환 개념이 앱에 아직 없어(#166 참고) 첫 번째 장소로 임시 고정한다.
    private var resolvedPlaceId: Long? = null

    private suspend fun resolvePlaceId(): Long? {
        resolvedPlaceId?.let { return it }
        val resolved = getPlaceListUseCase().getOrNull()?.firstOrNull()?.placeId
        resolvedPlaceId = resolved
        return resolved
    }

    // 캐시해둔 placeId가 가리키는 장소가 그 사이 삭제되는 등으로 이 값을 쓰는 호출이 실패하면
    // 캐시를 지워서 다음 진입 시 장소 목록을 다시 조회하게 한다 - 캐시가 죽은 채로 남아있으면
    // 장소가 삭제된 뒤에도 이 화면 전체가 계속 실패한다(#232).
    private fun invalidatePlaceId() {
        resolvedPlaceId = null
    }

    fun onIntent(intent: AdminFeedbackIntent) {
        when (intent) {
            is AdminFeedbackIntent.LoadInitial -> checkInitialState(intent.feedbackId, intent.openQr)
            is AdminFeedbackIntent.NavigateBack -> navigationManager.navigateBack()
            is AdminFeedbackIntent.NavigateToFeedbackDetail -> navigateToFeedbackDetail(intent)
            is AdminFeedbackIntent.NavigateToQR -> navigateToQR()
            is AdminFeedbackIntent.ClickTab -> handleTabClick(intent.tab)
            is AdminFeedbackIntent.UpdateStatus -> updateStatus(intent.feedbackId, intent.approve)
            is AdminFeedbackIntent.DownloadQr -> downloadQr()
            is AdminFeedbackIntent.LinkCopied ->
                navigationManager.emitEvent(AdminFeedbackEvent.ShowSnackbar("링크를 복사했어요."))
        }
    }

    private fun downloadQr() {
        val state = navigationManager.uiState.value as? AdminFeedbackUiState.FeedbackQR ?: return
        val qrImageUrl = state.qrImageUrl() ?: return

        viewModelScope.launch {
            val saved = galleryImageSaver.saveFromUrl(qrImageUrl, "reborn_qr_${state.placeId}")
            if (saved) {
                navigationManager.emitEvent(AdminFeedbackEvent.ShowSnackbar("갤러리에 저장했어요."))
            } else {
                navigationManager.emitEvent(
                    AdminFeedbackEvent.ShowErrorSnackbar(IllegalStateException("이미지 저장에 실패했어요."))
                )
            }
        }
    }

    private fun checkInitialState(feedbackId: Int? = null, openQr: Boolean = false) {
        navigationManager.clearAndReset(AdminFeedbackUiState.Loading)
        viewModelScope.launch {
            val placeId = resolvePlaceId()
            if (placeId == null) {
                navigationManager.emitEvent(
                    AdminFeedbackEvent.ShowErrorSnackbar(IllegalStateException("등록된 장소가 없습니다."))
                )
                navigationManager.clearAndReset(AdminFeedbackUiState.Feedback(emptyList()))
                return@launch
            }
            getFeedbackListUseCase(placeId)
                .onSuccess { list ->
                    feedbacks = list.map { it.toFeedbackItem() }
                    navigationManager.clearAndReset(AdminFeedbackUiState.Feedback(feedbacks))
                    if (feedbackId != null) {
                        navigateToFeedbackDetail(AdminFeedbackIntent.NavigateToFeedbackDetail(feedbackId))
                    } else if (openQr) {
                        navigateToQR()
                    }
                }
                .onFailure {
                    invalidatePlaceId()
                    navigationManager.emitEvent(AdminFeedbackEvent.ShowErrorSnackbar(it))
                    navigationManager.clearAndReset(AdminFeedbackUiState.Feedback(emptyList()))
                }
        }
    }

    // QR 화면으로 즉시 전환(로딩 표시)한 뒤, 장소의 qrCode를 조회해서 방문자용 피드백 웹페이지
    // URL을 완성한다 - QR 이미지는 별도 라이브러리 없이 공개 QR 생성 API로 렌더링(#163)
    private fun navigateToQR() {
        viewModelScope.launch {
            val placeId = resolvePlaceId()
            if (placeId == null) {
                navigationManager.emitEvent(
                    AdminFeedbackEvent.ShowErrorSnackbar(IllegalStateException("등록된 장소가 없습니다."))
                )
                return@launch
            }
            navigationManager.navigateTo(AdminFeedbackUiState.FeedbackQR(placeId.toInt()))
            getPlaceDetailUseCase(placeId)
                .onSuccess { detail ->
                    navigationManager.updateCurrentState { state ->
                        (state as? AdminFeedbackUiState.FeedbackQR)?.copy(qrUrl = detail.qrUrl) ?: state
                    }
                }
                .onFailure {
                    invalidatePlaceId()
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

    private fun updateStatus(feedbackId: Int, approve: Boolean) {
        val target = feedbacks.find { it.id == feedbackId } ?: return
        val nextState = if (approve) State.APPROVE else State.REJECT
        val statusParam = if (approve) "APPROVED" else "REJECTED"

        viewModelScope.launch {
            updateFeedbackStatusUseCase(feedbackId.toLong(), statusParam)
                .onSuccess { controlSent ->
                    feedbacks = feedbacks.map { item ->
                        if (item.id == feedbackId) item.copy(state = nextState) else item
                    }
                    navigationManager.navigateBack()
                    navigationManager.updateCurrentState { state ->
                        (state as? AdminFeedbackUiState.Feedback)?.copy(feedbacks = feedbacks) ?: state
                    }
                    if (approve && controlSent) {
                        navigationManager.emitEvent(AdminFeedbackEvent.ShowSnackbar("승인 완료 — 기기에 전송했어요."))
                    }
                }
                .onFailure {
                    navigationManager.emitEvent(AdminFeedbackEvent.ShowErrorSnackbar(it))
                }
        }
    }

    private fun handleTabClick(tab: AdminFeedbackUiState.FeedbackFiltering) {
        navigationManager.updateCurrentState { state ->
            if (state is AdminFeedbackUiState.Feedback) {
                state.copy(feedbackFiltering = tab)
            } else state
        }
    }

    private fun Feedback.toFeedbackItem(): AdminFeedbackUiState.FeedbackItem =
        AdminFeedbackUiState.FeedbackItem(
            id = feedbackId.toInt(),
            type = classifyFeedbackType(content),
            state = feedbackStatusToState(status),
            title = content,
            time = formatFeedbackRelativeTime(createdAt),
            submittedAt = formatAbsoluteTime(createdAt),
            content = content,
            sensorSnapshot = toSensorSnapshot(),
            temperatureAdjustment = toTemperatureAdjustment(),
            aiAdvice = aiAdvice,
        )

    // 네 값이 전부 있어야 Figma 596:3594의 4개 칩(온도/습도/조도/재실 인원)을 온전히 채울 수 있음 -
    // 하나라도 없으면(제출 직후 아직 비동기 처리 전 등) 섹션 자체를 숨긴다.
    private fun Feedback.toSensorSnapshot(): AdminFeedbackUiState.SensorSnapshot? {
        val temperature = snapshotTemperature ?: return null
        val humidity = snapshotHumidity ?: return null
        val illuminance = snapshotIlluminance ?: return null
        val peopleCount = snapshotPeopleCount ?: return null
        return AdminFeedbackUiState.SensorSnapshot(
            temperature = temperature,
            humidity = humidity,
            illuminance = illuminance,
            peopleCount = peopleCount,
        )
    }

    private fun Feedback.toTemperatureAdjustment(): AdminFeedbackUiState.TemperatureAdjustment? {
        val before = recommendedTemperatureBefore ?: return null
        val after = recommendedTemperatureAfter ?: return null
        return AdminFeedbackUiState.TemperatureAdjustment(before = before, after = after)
    }

    private fun formatAbsoluteTime(iso: String): String {
        val dt = LocalDateTime.parse(iso)
        return "${dt.year}.${dt.monthNumber.pad2()}.${dt.dayOfMonth.pad2()} ${dt.hour.pad2()}:${dt.minute.pad2()}"
    }

    private fun Int.pad2(): String = toString().padStart(2, '0')
}
