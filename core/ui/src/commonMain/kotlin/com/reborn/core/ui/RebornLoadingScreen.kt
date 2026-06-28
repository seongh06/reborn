package com.reborn.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.ext.rebornDefault


@Composable
fun RebornLoadingScreen() {
    Box(
        modifier = Modifier.rebornDefault(Color.White).fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "로딩중입니다.",
            style = RebornTheme.typography.displayLarge,
            color = RebornTheme.color.grayScale900
        )
    }
}