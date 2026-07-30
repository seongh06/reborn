package com.reborn.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.Res
import com.reborn.core.ui.ic_airConditioner
import com.reborn.core.ui.ic_curtain
import com.reborn.core.ui.ic_device
import com.reborn.core.ui.ic_kamp
import com.reborn.core.ui.ic_plug
import com.reborn.core.ui.ic_power
import com.reborn.core.ui.ic_tv
import org.jetbrains.compose.resources.painterResource

enum class DeviceType {
    LAMP,
    PLUG,
    TV,
    AIR_CONDITIONER,
    CURTAIN,
    OTHER
}

val DeviceType.icon
    get() = when (this) {
        DeviceType.LAMP -> Res.drawable.ic_kamp
        DeviceType.PLUG -> Res.drawable.ic_plug
        DeviceType.TV -> Res.drawable.ic_tv
        DeviceType.AIR_CONDITIONER -> Res.drawable.ic_airConditioner
        DeviceType.CURTAIN -> Res.drawable.ic_curtain
        DeviceType.OTHER -> Res.drawable.ic_device
    }

@Composable
fun DeviceListItem(
    place: String,
    name: String,
    isOnline: Boolean,
    isPowerOn: Boolean,
    deviceType: DeviceType = DeviceType.OTHER,
    onPowerToggle: () -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPowerOn) RebornTheme.color.grayScale200 else RebornTheme.color.grayScale500)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(deviceType.icon),
                modifier = Modifier.size(32.dp),
                contentDescription = null,
                tint = if (isOnline) RebornTheme.color.grayScale900 else RebornTheme.color.grayScale400
            )
            DeviceOnOffButton(
                isPowerOn = isPowerOn,
                isOnline = isOnline,
                onClick = onPowerToggle
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = name,
                style = RebornTheme.typography.titleMedium,
                color = RebornTheme.color.grayScale900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$place · ${if (isOnline) "온라인" else "오프라인"}",
                style = RebornTheme.typography.caption,
                color = if (isOnline) RebornTheme.color.grayScale700 else RebornTheme.color.reject
            )
        }
    }
}

// Figma(595:5067 등 adjust_item)는 전원 버튼을 카드 배경과 같은 색으로 블렌딩된 원형 아이콘
// 버튼으로 그린다 - 채워진 원으로 온/오프를 표현하던 기존 디자인 대신, 아이콘 자체의 색상
// (진하게/흐리게)만으로 상태를 표현한다(#235).
@Composable
fun DeviceOnOffButton(
    isPowerOn: Boolean,
    isOnline: Boolean,
    onClick: () -> Unit
){
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ){
        Icon(
            painter = painterResource(Res.drawable.ic_power),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (isPowerOn && isOnline) RebornTheme.color.grayScale900 else RebornTheme.color.grayScale500
        )
    }
}