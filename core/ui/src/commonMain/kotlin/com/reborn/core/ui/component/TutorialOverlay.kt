package com.reborn.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.ext.toRect

// 최초 접속 튜토리얼(#240) 공용 컴포넌트 - 화면을 어둡게 덮고 highlightRect 부분만 뚫어서
// 강조한 뒤, 뚫린 부분 위/아래(공간이 남는 쪽)에 설명 문구를 보여준다. 화면 아무 곳이나 탭하면
// 다음 단계로 넘어간다(Yakssok_Android #65/#66 패턴 참고).
@Composable
fun TutorialSpotlightOverlay(
    highlightRect: Rect?,
    explanation: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
) {
    val density = LocalDensity.current
    val screenHeightPx = LocalWindowInfo.current.containerSize.height

    var explainCardHeightPx by remember { mutableStateOf(0) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val overlayRadiusPx = with(density) { cornerRadius.toPx() }
    val overlayPath = remember(highlightRect, canvasSize, overlayRadiusPx) {
        Path().apply {
            addRect(Rect(0f, 0f, canvasSize.width, canvasSize.height))
            highlightRect?.let {
                addRoundRect(RoundRect(rect = it, cornerRadius = CornerRadius(overlayRadiusPx, overlayRadiusPx)))
            }
            fillType = PathFillType.EvenOdd
        }
    }

    // 하이라이트 아래쪽에 설명 카드를 놓을 공간이 부족하면(화면 하단부 강조) 위쪽에 놓는다.
    val placeBelow = highlightRect == null ||
        (screenHeightPx - highlightRect.bottom) > (explainCardHeightPx + 48).coerceAtLeast(160)

    val interactionSource = remember { MutableInteractionSource() }
    // DrawScope 람다는 @Composable 컨텍스트가 아니라 RebornTheme.color를 그 안에서 바로 못
    // 읽는다 - Canvas 밖에서 먼저 읽어서 캡처해야 한다.
    val overlayColor = RebornTheme.color.grayScale900.copy(alpha = 0.8f)

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size -> canvasSize = Size(size.width.toFloat(), size.height.toFloat()) }
                .clickable(interactionSource = interactionSource, indication = null, onClick = onDismiss)
        ) {
            drawPath(path = overlayPath, color = overlayColor)
        }

        val alignment = if (placeBelow) Alignment.TopStart else Alignment.BottomStart
        val topPaddingDp = if (placeBelow && highlightRect != null) {
            with(density) { highlightRect.bottom.toDp() } + 16.dp
        } else 0.dp
        val bottomPaddingDp = if (!placeBelow && highlightRect != null) {
            with(density) { (screenHeightPx - highlightRect.top).toDp() } + 16.dp
        } else 0.dp

        Text(
            text = explanation,
            style = RebornTheme.typography.bodyMedium,
            color = RebornTheme.color.grayScale100,
            modifier = Modifier
                .align(alignment)
                .padding(start = 24.dp, end = 24.dp, top = topPaddingDp, bottom = bottomPaddingDp)
                .onSizeChanged { explainCardHeightPx = it.height }
                .clip(RoundedCornerShape(12.dp))
                .background(RebornTheme.color.grayScale800)
                .padding(16.dp)
        )
    }
}

// 하이라이트 대상 컴포저블에 붙여서 화면 루트 기준 위치를 얻는다.
@Composable
fun Modifier.tutorialTarget(onMeasured: (Rect) -> Unit): Modifier {
    val density = LocalDensity.current
    return this.onGloballyPositioned { onMeasured(it.toRect(density)) }
}
