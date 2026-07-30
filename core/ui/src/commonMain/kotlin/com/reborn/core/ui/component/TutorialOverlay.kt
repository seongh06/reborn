package com.reborn.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.translate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.ext.toRect

// 최초 접속 튜토리얼(#240) 공용 컴포넌트 - 화면을 어둡게 덮고 highlightRect 부분만 뚫어서
// 강조한다. 설명 문구는 화면마다 다른 위치에 뜨는 대신 App.kt(바텀 네비 자리)에서 별도로
// 보여준다 - TutorialHintCard 참고. 화면 아무 곳이나 탭하면 다음 단계로 넘어간다.
@Composable
fun TutorialHighlightOverlay(
    highlightRect: Rect?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    // 하이라이트 대상(텍스트+자체 padding) 딱 그대로면 너무 빡빡해 보여서, 여기서 한 번 더
    // 살짝 여유를 준다.
    highlightPadding: Dp = 4.dp,
    dimAlpha: Float = 0.55f,
) {
    val density = LocalDensity.current
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    // highlightRect는 tutorialTarget()이 positionInRoot()로 잰 "화면 루트 기준" 좌표라, 이
    // Canvas 자신의 로컬 좌표계와 다를 수 있다(예: 상위에 레터박싱 Box가 있는 경우) - Canvas
    // 자신의 루트 기준 위치를 빼서 로컬 좌표로 보정해야 정확한 자리에 구멍이 뚫린다.
    var canvasRootOffset by remember { mutableStateOf(Offset.Zero) }

    val overlayRadiusPx = with(density) { cornerRadius.toPx() }
    val highlightPaddingPx = with(density) { highlightPadding.toPx() }
    val localHighlightRect = remember(highlightRect, canvasRootOffset, highlightPaddingPx) {
        highlightRect?.translate(-canvasRootOffset.x, -canvasRootOffset.y)?.let {
            Rect(
                left = it.left - highlightPaddingPx,
                top = it.top - highlightPaddingPx,
                right = it.right + highlightPaddingPx,
                bottom = it.bottom + highlightPaddingPx
            )
        }
    }
    val overlayPath = remember(localHighlightRect, canvasSize, overlayRadiusPx) {
        Path().apply {
            addRect(Rect(0f, 0f, canvasSize.width, canvasSize.height))
            localHighlightRect?.let {
                addRoundRect(RoundRect(rect = it, cornerRadius = CornerRadius(overlayRadiusPx, overlayRadiusPx)))
            }
            fillType = PathFillType.EvenOdd
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    // DrawScope 람다는 @Composable 컨텍스트가 아니라 RebornTheme.color를 그 안에서 바로 못
    // 읽는다 - Canvas 밖에서 먼저 읽어서 캡처해야 한다.
    val overlayColor = RebornTheme.color.grayScale900.copy(alpha = dimAlpha)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                canvasRootOffset = coordinates.positionInRoot()
                canvasSize = coordinates.size.toSize()
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onDismiss)
    ) {
        drawPath(path = overlayPath, color = overlayColor)
    }
}

// 튜토리얼 설명 문구 카드 - 바텀 네비게이션 자리(App.kt)에 대신 뜬다. 하얀 배경 + 큰 라운드로
// 눈에 띄게, 글자는 titleSmall로 충분히 크게.
@Composable
fun TutorialHintCard(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = RebornTheme.typography.titleSmall,
        color = RebornTheme.color.grayScale900,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    )
}

// 하이라이트 대상 컴포저블에 붙여서 화면 루트 기준 위치를 얻는다.
@Composable
fun Modifier.tutorialTarget(onMeasured: (Rect) -> Unit): Modifier {
    val density = LocalDensity.current
    return this.onGloballyPositioned { onMeasured(it.toRect(density)) }
}
