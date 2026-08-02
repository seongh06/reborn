package com.reborn.feature.admin.feedback

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.feedback.model.AdminFeedbackUiState

// Figma에 QR 화면 프레임은 없음 - 방문자용 피드백 웹페이지(#163) URL을 QR 이미지로 보여주고
// 공유할 수 있게 하는 것이 목적. QR 이미지 자체는 별도 라이브러리 없이 공개 QR 생성 API로 렌더링
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdminFeedbackQRScreen(
    state: AdminFeedbackUiState.FeedbackQR,
    onBackClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onLinkCopied: () -> Unit = {}
){
    val qrUrl = state.qrUrl
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier.rebornDefault(Color.White, topPadding = false)
    ){
        RebornTopAppBar(title = "QR 코드", onBackClick = onBackClick)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ){
            if (state.failed) {
                Text(
                    modifier = Modifier.padding(24.dp),
                    text = "QR 코드를 불러오지 못했어요. 다시 시도해주세요.",
                    style = RebornTheme.typography.bodyMedium,
                    color = RebornTheme.color.grayScale700,
                    textAlign = TextAlign.Center
                )
            } else if (qrUrl == null) {
                CircularProgressIndicator(color = RebornTheme.color.grayScale900)
            } else {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = state.qrImageUrl(),
                        contentDescription = "피드백 웹페이지 QR 코드",
                        modifier = Modifier.size(240.dp)
                    )
                    Text(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { uriHandler.openUri(qrUrl) },
                                onLongClick = {
                                    clipboardManager.setText(AnnotatedString(qrUrl))
                                    onLinkCopied()
                                }
                            ),
                        text = "웹페이지 바로가기",
                        style = RebornTheme.typography.bodyMedium,
                        color = RebornTheme.color.grayScale700,
                        textDecoration = TextDecoration.Underline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        RebornButton(
            text = "이미지 다운로드",
            enabled = qrUrl != null,
            onClick = { onDownloadClick() }
        )
    }
}
