package com.reborn.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.ext.rebornDefault

@Composable
fun RebornLoadingScreen() {
    Box(
        modifier = Modifier.rebornDefault(Color.White).fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingDotsIndicator()
    }
}

// 점 3개가 순서대로 위아래로 통통 튀는 로딩 인디케이터 - "로딩중입니다." 텍스트를 대체(#177).
// 외부 애니메이션 라이브러리(Lottie 등) 없이 Compose infiniteTransition만으로 구현 - 자산/라이선스
// 관리가 필요 없고, 점 색상도 디자인 시스템 토큰을 그대로 써서 흑백 톤을 유지할 수 있음.
@Composable
private fun LoadingDotsIndicator(
    dotSize: androidx.compose.ui.unit.Dp = 12.dp,
    dotColor: Color = RebornTheme.color.grayScale900,
    bounceHeight: androidx.compose.ui.unit.Dp = 10.dp
) {
    val transition = rememberInfiniteTransition(label = "loading_dots")
    val durationMs = 600
    val delayPerDot = 120

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = durationMs
                        0f at 0
                        1f at durationMs / 2
                        0f at durationMs
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(delayPerDot * index)
                ),
                label = "dot_offset_$index"
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .offset(y = -bounceHeight * progress)
                    .background(dotColor, CircleShape)
            )
        }
    }
}
