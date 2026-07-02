package com.reborn.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.Res
import com.reborn.core.ui.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun DeviceListItem(
    place: String,
    name: String,
    isOnline: Boolean,
    isPowerOn: Boolean,
    onPowerToggle: () -> Unit
) {
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_device),
                modifier = Modifier.size(32.dp),
                contentDescription = null,
                tint = if (isOnline) RebornTheme.color.grayScale900 else RebornTheme.color.grayScale400
            )
            Switch(
                checked = isPowerOn,
                onCheckedChange = { onPowerToggle() },
                enabled = isOnline,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = RebornTheme.color.approve
                )
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = name,
                style = RebornTheme.typography.titleMedium,
                color = RebornTheme.color.grayScale900
            )
            Text(
                text = "$place · ${if (isOnline) "온라인" else "오프라인"}",
                style = RebornTheme.typography.labelMedium,
                color = if (isOnline) RebornTheme.color.grayScale600 else RebornTheme.color.reject
            )
        }
    }
}
