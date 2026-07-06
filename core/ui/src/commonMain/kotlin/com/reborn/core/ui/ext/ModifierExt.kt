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
        .customInsets(top = true, bottom = bottomPadding)
        .run {
            if (bottomPadding) this.padding(bottom = 24.dp) else this
        }
}