package com.reborn.feature.admin.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.setting.component.RoomListItem

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp,8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ){
            Text(
                text = "Place",
                style = RebornTheme.typography.titleMedium,
                color = RebornTheme.color.grayScale900
            )
            RoomListItem(
                placeId = 1,
                roomId = 1,
                roomName = "Home 01",
                adminCount = 3,
                deviceCount = 3,
                onDeleteClick = {},
                onAddAdminClick = {},
                onAddDeviceClick = {}
            )
            RoomListItem(
                placeId = 2,
                roomId = 2,
                roomName = "Cafe",
                adminCount = 2,
                deviceCount = 5,
                onDeleteClick = {},
                onAddAdminClick = {},
                onAddDeviceClick = {}
            )
        }
    }
}
