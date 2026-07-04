package com.reborn.feature.admin.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.component.FeedbackItem
import com.reborn.core.ui.component.FeedbackType
import com.reborn.core.ui.component.State
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.feedback.model.AdminFeedbackUiState

@Composable
fun AdminFeedbackDetailScreen(
    state: AdminFeedbackUiState.FeedbackDetail,
    onBackClick: () -> Unit,
    onRejectClick: () -> Unit,
    onApproveClick: () -> Unit
){
    val feedbackDetail = AdminFeedbackUiState.FeedbackItem(1, FeedbackType.AIR, State.APPROVE, "너무 바람이 강해요", "2026.06.11 10:24", "오늘 소나기가 오는데 창문이 열려있어서 너무 춥습니다. 창문을 닫거나 에어컨 온도를 올리면 좋을거같아요.")

    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ){
        RebornTopAppBar(title = "피드백 상세보기", onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier.padding(16.dp, 8.dp)
            ) {
                FeedbackItem(
                    id = 1,
                    state = State.APPROVE,
                    time = "5분전",
                    title = "피드백 제목",
                    type = FeedbackType.AIR,
                    onClick = { /*onFeedbackClick*/ }
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ){
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        feedbackDetail.title,
                        style = RebornTheme.typography.titleLarge,
                        color = RebornTheme.color.grayScale900
                    )
                    Text(
                        feedbackDetail.time,
                        style = RebornTheme.typography.caption,
                        color = RebornTheme.color.grayScale900
                    )
                }
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    text = feedbackDetail.content,
                    style = RebornTheme.typography.bodyLarge,
                    color = RebornTheme.color.grayScale900
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(RebornTheme.color.grayScale200)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .padding(16.dp, 32.dp),
                contentAlignment = Alignment.Center
            ){
                Text(
                    feedbackDetail.content,
                    style = RebornTheme.typography.bodyMedium,
                    color = RebornTheme.color.grayScale900
                )
            }
        }

        RebornButton(
            text = "승인",
            onClick = { onApproveClick() }
        )
        RebornButton(
            text = "거절",
            onClick = { onRejectClick() },
            backgroundColor = RebornTheme.color.grayScale400
        )
    }
}