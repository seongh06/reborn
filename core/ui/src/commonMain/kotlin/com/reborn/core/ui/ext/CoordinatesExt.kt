package com.reborn.core.ui.ext

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 튜토리얼 하이라이트용 - 화면 루트 기준 좌표로 변환한다. padding을 주면 하이라이트 구멍이
// 실제 컴포넌트보다 살짝 넉넉하게 뚫린다(#240).
fun LayoutCoordinates.toRect(
    density: Density,
    verticalPadding: Dp = 0.dp,
    horizontalPadding: Dp = 0.dp,
): Rect {
    val verticalPaddingPx = with(density) { verticalPadding.toPx() }
    val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }
    val pos = positionInRoot()

    return Rect(
        left = pos.x - horizontalPaddingPx,
        top = pos.y - verticalPaddingPx,
        right = pos.x + size.width + horizontalPaddingPx,
        bottom = pos.y + size.height + verticalPaddingPx,
    )
}
