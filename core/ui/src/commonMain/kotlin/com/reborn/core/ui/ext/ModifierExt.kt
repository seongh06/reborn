package com.reborn.core.ui.ext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.customInsets(
    top: Boolean = false,
    bottom: Boolean = false,
    // 시스템 제스처/내비게이션 바 inset이 이 값보다 작을 때 최소로 보장할 여백 - union()은
    // 각 변의 inset 중 "더 큰 쪽"을 취하는 것이라(합산이 아님) 시스템 inset과 겹치지 않고
    // 항상 최소 이만큼의 여백을 보장한다.
    minBottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
): Modifier {
    var m = this
    if (top) m = m.windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
            .union(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            .union(WindowInsets.systemGestures.only(WindowInsetsSides.Top))
    )
    if (bottom) m = m.windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
            .union(WindowInsets.tappableElement.only(WindowInsetsSides.Bottom))
            .union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom))
            .union(WindowInsets(bottom = minBottomPadding))
    )
    return m
}

@Composable
fun Modifier.rebornDefault(
    color: Color = Color.White,
    bottomPadding: Boolean = true
): Modifier {
    return Modifier
        .background(color)
        .fillMaxSize()
        .then(this)
        // 이전에는 시스템 inset을 windowInsetsPadding으로 적용한 뒤 24dp를 또 padding()으로
        // 더해서, 제스처 내비게이션 기기에서 시스템 inset(보통 16~24dp) + 24dp가 합산돼
        // 하단 여백이 두 배로 넓어 보이는 버그가 있었다(발견 07-28). union()으로 "더 큰 값"만
        // 취하도록 고쳐서 항상 최소 24dp, 시스템 inset이 더 크면 그 값만 적용되게 수정.
        .customInsets(top = true, bottom = bottomPadding, minBottomPadding = if (bottomPadding) 24.dp else 0.dp)
}