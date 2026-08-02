package com.reborn.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.ext.customInsets

// Scaffold의 snackbarHost 슬롯은 위치가 하단으로 고정돼있어(SubcomposeLayout이 직접 좌표를
// 계산해 배치) 슬롯 안에서 Modifier.align 등을 줘도 위치를 못 바꾼다 - 그래서 Scaffold 바깥에
// Box로 감싸고 SnackbarHost를 별도 형제 노드로 얹어 상단에 띄운다. 화면마다 Scaffold(snackbarHost
// = {...}) { padding -> ... } 를 이걸로만 바꾸면 내부 content 들여쓰기는 그대로 유지된다.
@Composable
fun RebornScaffold(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(modifier = modifier) {
        Scaffold(content = content)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .customInsets(top = true)
                .padding(top = 8.dp)
        ) { data ->
            // 아무리 긴 메시지도 최대 2줄까지만 보여주고 넘치면 말줄임(...) 처리. 배경은 브랜드
            // 흑백 팔레트에 맞춰 거의 불투명한 회색으로, 글씨는 기본보다 크게 키운다.
            Snackbar(
                modifier = Modifier.padding(horizontal = 16.dp).sizeIn(minHeight = 80.dp).clip(RoundedCornerShape(20.dp)),
                containerColor = RebornTheme.color.grayScale700.copy(alpha = 0.94f),
                contentColor = RebornTheme.color.grayScale100,
            ) {
                Box(contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = data.visuals.message,
                        style = RebornTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
