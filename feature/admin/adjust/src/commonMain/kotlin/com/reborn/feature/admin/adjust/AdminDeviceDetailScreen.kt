package com.reborn.feature.admin.adjust

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.adjust.model.AdminAdjustUiState

@Composable
fun AdminDeviceDetailScreen(
    state: AdminAdjustUiState.DeviceDetail,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ){
        RebornTopAppBar(title = "IoT 기기 상세보기", onBackClick = onBackClick)
    }
}