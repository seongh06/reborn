package com.reborn.core.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import com.reborn.core.ui.ic_aerometer
import com.reborn.core.ui.ic_aiSpeaker
import com.reborn.core.ui.ic_airConditioner
import com.reborn.core.ui.ic_airPurifier
import com.reborn.core.ui.ic_arduino
import com.reborn.core.ui.ic_device
import com.reborn.core.ui.ic_kamp
import com.reborn.core.ui.ic_plug
import com.reborn.core.ui.ic_power
import com.reborn.core.ui.ic_tv
import com.reborn.core.ui.ic_ventilator
import org.jetbrains.compose.resources.painterResource

enum class DeviceType {
    LAMP,
    PLUG,
    TV,
    AIR_CONDITIONER,
    AIR_PURIFIER,
    VENTILATOR,
    // 아래 3개는 category(SmartThings 전용)가 아니라 device.deviceType 기준으로 직접 매핑된다 -
    // ARDUINO/AEROMETER/AI_SPEAKER는 category가 항상 null이라 예전엔 전부 OTHER로만 표시됐음(#298).
    ARDUINO,
    AI_SPEAKER,
    AEROMETER,
    OTHER
}

val DeviceType.icon
    get() = when (this) {
        DeviceType.LAMP -> Res.drawable.ic_kamp
        DeviceType.PLUG -> Res.drawable.ic_plug
        DeviceType.TV -> Res.drawable.ic_tv
        DeviceType.AIR_CONDITIONER -> Res.drawable.ic_airConditioner
        DeviceType.AIR_PURIFIER -> Res.drawable.ic_airPurifier
        DeviceType.VENTILATOR -> Res.drawable.ic_ventilator
        DeviceType.ARDUINO -> Res.drawable.ic_arduino
        DeviceType.AI_SPEAKER -> Res.drawable.ic_aiSpeaker
        DeviceType.AEROMETER -> Res.drawable.ic_aerometer
        DeviceType.OTHER -> Res.drawable.ic_device
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceListItem(
    place: String,
    name: String,
    isOnline: Boolean,
    isPowerOn: Boolean,
    deviceType: DeviceType = DeviceType.OTHER,
    onPowerToggle: () -> Unit,
    onClick: () -> Unit,
    // 기기 리스트(#327)에서 롱클릭 시 삭제 바텀시트를 띄우기 위한 훅 - 다른 화면(Home 대시보드,
    // Adjust)은 안 넘기면 기존과 동일하게 동작하도록 기본값 없음(no-op).
    onLongClick: () -> Unit = {}
) {
    // 배경이 isPowerOn 기준으로 어둡게/밝게 반전되므로(#235 CodeRabbit 리뷰), 아이콘/이름
    // 색상도 같은 기준으로 반전해야 켜짐(어두운 배경) 상태에서 글자가 묻히지 않는다.
    val foregroundColor = when {
        !isOnline -> RebornTheme.color.grayScale400
        isPowerOn -> RebornTheme.color.grayScale100
        else -> RebornTheme.color.grayScale900
    }
    val subtitleColor = when {
        !isOnline -> RebornTheme.color.reject
        isPowerOn -> RebornTheme.color.grayScale300
        else -> RebornTheme.color.grayScale700
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPowerOn) RebornTheme.color.grayScale500 else RebornTheme.color.grayScale200)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
                tint = foregroundColor
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
                color = foregroundColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$place · ${if (isOnline) "온라인" else "오프라인"}",
                style = RebornTheme.typography.caption,
                color = subtitleColor
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
            // 카드 배경(위에서 isPowerOn 기준으로 어둡게/밝게 반전)과 항상 반대색이어야 아이콘이
            // 배경에 묻히지 않는다 - 꺼짐/오프라인일 땐 밝은 배경 위 어두운 아이콘, 켜짐일 땐
            // 어두운 배경 위 밝은 아이콘.
            tint = when {
                !isOnline -> RebornTheme.color.grayScale400
                isPowerOn -> RebornTheme.color.grayScale100
                else -> RebornTheme.color.grayScale700
            }
        )
    }
}