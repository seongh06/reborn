package com.reborn.feature.admin.adjust.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.component.icon
import com.reborn.feature.admin.adjust.model.Device
import org.jetbrains.compose.resources.painterResource

@Composable
fun DeviceSection(
    device: Device
){
    Row(
        modifier = Modifier
            .padding(12.dp,8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RebornTheme.color.grayScale100)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ){
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Icon(
                painter = painterResource(device.deviceType.icon),
                modifier = Modifier.size(24.dp),
                contentDescription = null,
                tint = RebornTheme.color.grayScale700
            )
            Column(
            ){
                Text(
                    device.place,
                    style = RebornTheme.typography.labelLarge,
                    color = RebornTheme.color.grayScale700
                )
                Text(
                    device.name,
                    style = RebornTheme.typography.labelMedium,
                    color = RebornTheme.color.grayScale700
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (device.isOnline) RebornTheme.color.approve else RebornTheme.color.reject)
        )
    }
}