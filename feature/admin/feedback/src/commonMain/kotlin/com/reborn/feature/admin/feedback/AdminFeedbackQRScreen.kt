package com.reborn.feature.admin.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.feedback.model.AdminFeedbackUiState

@Composable
fun AdminFeedbackQRScreen(
    state: AdminFeedbackUiState.FeedbackQR,
    onBackClick: () -> Unit,
    onDownloadClick: () -> Unit
){
    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ){
        RebornTopAppBar(title = "QR 코드", onBackClick = onBackClick)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ){
            // TODO: QR 코드 이미지 생성/표시 및 "이미지 다운로드" 저장 로직은 후속 이슈에서 구현 예정
            Box(
                modifier = Modifier.padding(40.dp)
            ){
            }
        }
        RebornButton(
            text = "이미지 다운로드",
            onClick = { onDownloadClick() }
        )
    }
}