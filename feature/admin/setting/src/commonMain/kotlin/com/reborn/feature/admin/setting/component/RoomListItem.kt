package com.reborn.feature.admin.setting.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.feature.admin.setting.Res
import com.reborn.feature.admin.setting.ic_admin
import com.reborn.feature.admin.setting.ic_delete
import com.reborn.feature.admin.setting.ic_device
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun RoomListItem(
    placeId: Int,
    roomName: String,
    adminCount: Int,
    deviceCount: Int?,
    onDeleteClick: () -> Unit,
    onAddAdminClick: () -> Unit,
    onAddDeviceClick: () -> Unit,
    onAddArduinoClick: () -> Unit
){
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RebornTheme.color.grayScale100)
            .border(
                width = 1.dp,
                color = RebornTheme.color.grayScale200,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ){
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ){
            Row {
                Text(
                    roomName,
                    style = RebornTheme.typography.headlineMedium,
                    color = RebornTheme.color.grayScale900
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painterResource(Res.drawable.ic_delete),
                    modifier = Modifier.size(24.dp).clickable(onClick = onDeleteClick),
                    contentDescription = null,
                    tint = RebornTheme.color.grayScale900
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ){
                RoomInformChip(type = RoomInformType.Admin, value = adminCount)
                RoomInformChip(type = RoomInformType.IoT, value = deviceCount)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ){
            RebornButton(
                modifier = Modifier.weight(1f),
                text = "관리자 초대",
                backgroundColor = RebornTheme.color.grayScale300,
                onClick = onAddAdminClick,
                contentPadding = PaddingValues(8.dp)
            )
            RebornButton(
                modifier = Modifier.weight(1f),
                text = "공기계 추가",
                backgroundColor = RebornTheme.color.grayScale300,
                onClick = onAddDeviceClick,
                contentPadding = PaddingValues(8.dp)
            )
            RebornButton(
                modifier = Modifier.weight(1f),
                text = "아두이노 추가",
                backgroundColor = RebornTheme.color.grayScale300,
                onClick = onAddArduinoClick,
                contentPadding = PaddingValues(8.dp)
            )

        }
    }
}

@Composable
private fun RoomInformChip(
    modifier: Modifier = Modifier,
    type: RoomInformType,
    value: Int?,
) {
    val style = getUiStyleForType(type)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(RebornTheme.color.grayScale300)
            .padding(12.dp, 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ){
        Icon(
            painterResource(style),
            modifier = Modifier.size(16.dp),
            contentDescription = null,
            tint = RebornTheme.color.grayScale900
        )
        Text(
            // 조회 실패(null)는 "-"로 표시 - 0대와 혼동되지 않도록
            value?.toString() ?: "-",
            style = RebornTheme.typography.labelLarge,
            color = RebornTheme.color.grayScale900
        )
    }
}

enum class RoomInformType {
    Admin, IoT
}

@Composable
private fun getUiStyleForType(type: RoomInformType): DrawableResource {
    return when (type) {
        RoomInformType.Admin -> Res.drawable.ic_admin
        RoomInformType.IoT -> Res.drawable.ic_device
    }
}