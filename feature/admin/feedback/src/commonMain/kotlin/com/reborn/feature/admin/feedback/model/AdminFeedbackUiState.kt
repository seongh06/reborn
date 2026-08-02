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

    // qrUrl==null && !failed면 조회 중(로딩) - Figma에 없는 화면이라 QR 코드 이미지는 공개 QR 생성 API로
    // 렌더링. failed=true면 조회가 끝내 실패한 것 - 로딩 스피너에 무한히 갇히지 않도록 별도 표시(CodeRabbit 리뷰)
    data class FeedbackQR(val placeId: Int, val qrUrl: String? = null, val failed: Boolean = false) : AdminFeedbackUiState {
        // QR 이미지는 별도 라이브러리 없이 공개 QR 생성 API로 렌더링(#163) - 화면(AsyncImage)과
        // 다운로드(GalleryImageSaver) 양쪽에서 동일한 URL을 써야 해서 한 곳에서만 조립한다.
        fun qrImageUrl(): String? = qrUrl?.let { "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=$it" }
    }
    data class FeedbackItem(
        val id: Int,
        val type: FeedbackType,
        val state: State,
        val title: String,
        val time: String, // 목록용 상대 시각(ex. "5분전")
        val submittedAt: String, // 상세용 절대 시각(ex. "2026.06.11 10:24") - Figma 596:3555
        val content: String,
        // 피드백 제출 시점에 서버가 비동기로 Gemini 호출 후 채움 - 접수 직후 잠깐은 null일 수 있고,
        // 그 기기의 최신 메트릭이 아예 없거나 Gemini 호출이 실패하면 계속 null로 남는다.
        // 상세 화면에서 null이면 해당 섹션을 숨긴다.
        val sensorSnapshot: SensorSnapshot? = null,
        val temperatureAdjustment: TemperatureAdjustment? = null,
        // temperatureAdjustment와 상호 배타적 - IoT로 직접 제어할 수 없는 피드백에 대한 AI 조언.
        // 이게 있으면 승인/거절 버튼 없이 조언 텍스트만 보여준다(제어할 대상이 없어서 승인 자체가
        // 성립하지 않음).
        val aiAdvice: String? = null,
    )

    // 피드백 접수 시점 센서 스냅샷 - Figma 596:3594 4개 칩(온도/습도/조도/재실 인원)
    data class SensorSnapshot(
        val temperature: Double,
        val humidity: Double,
        val illuminance: Int,
        val peopleCount: Int
    )

    // "AI 맞춤 피드백" 추천 조절값 - Figma 596:3628 "희망 온도: 24.0 → 25.0 (1 증가)"
    data class TemperatureAdjustment(
        val before: Double,
        val after: Double
    )
    enum class FeedbackFiltering(val filtering: String, val state: State? = null) {
        ALL("전체", null),
        UNREAD("안읽음", State.UNREAD),
        READ("읽음", State.READ),
        APPROVE("승인", State.APPROVE),
        REJECT("거절", State.REJECT)
    }
}

fun AdminFeedbackUiState.Feedback.filteredFeedbacks(): List<AdminFeedbackUiState.FeedbackItem> {
    val targetState = feedbackFiltering.state ?: return feedbacks
    return feedbacks.filter { it.state == targetState }
}

sealed interface AdminFeedbackIntent{
    // feedbackId가 있으면 목록 로드 후 바로 해당 상세로 이동(Home 딥링크, #177)
    // openQr이 true면 목록 로드 후 바로 QR 화면으로 이동(Setting place 더보기 딥링크)
    data class LoadInitial(val feedbackId: Int? = null, val openQr: Boolean = false) : AdminFeedbackIntent
    data object NavigateBack : AdminFeedbackIntent
    data object NavigateToQR : AdminFeedbackIntent
    data class NavigateToFeedbackDetail(val feedbackId : Int) : AdminFeedbackIntent
    data class ClickTab(val tab: AdminFeedbackUiState.FeedbackFiltering) : AdminFeedbackIntent
    data class UpdateStatus(val feedbackId: Int, val approve: Boolean) : AdminFeedbackIntent
    data object DownloadQr : AdminFeedbackIntent
    data object LinkCopied : AdminFeedbackIntent
}