package com.reborn.feature.admin.setting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.ui.ext.rebornDefault

@Composable
fun AdminSettingRoute(
    onBackClick: () -> Unit
) {
    AdminSettingScreen(onBackClick = onBackClick)
}

@Composable
fun AdminSettingScreen(
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ) {
        RebornTopAppBar(title = "설정", onBackClick = onBackClick)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Admin Setting")
        }
    }
}
