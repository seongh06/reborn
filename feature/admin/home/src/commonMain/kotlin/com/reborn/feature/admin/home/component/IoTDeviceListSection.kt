package com.reborn.feature.admin.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reborn.core.ui.component.DeviceListItem
import com.reborn.core.ui.component.DeviceType
import com.reborn.core.ui.component.SectionTitleComponent

data class IoTDeviceItem(
    val id: Int,
    val place: String,
    val name: String,
    val isOnline: Boolean,
    val isPowerOn: Boolean,
    val deviceType: DeviceType = DeviceType.OTHER
)

// TODO: 서버 device API 연동 전까지의 목업 데이터. 실제 연동 시 UseCase로 대체 예정
// (feature:admin:adjust의 AdminAdjustViewModel과 동일한 목업 — Adjust 화면의 "연결된 기기" 그리드가
// Home의 이 섹션으로 흡수됨, #154)
private val mockDevices = listOf(
    IoTDeviceItem(1, "거실", "거실 조명", isOnline = true, isPowerOn = true, deviceType = DeviceType.LAMP),
    IoTDeviceItem(2, "거실", "거실 공기청정기", isOnline = true, isPowerOn = false, deviceType = DeviceType.AIR_CONDITIONER),
    IoTDeviceItem(3, "안방", "안방 가습기", isOnline = false, isPowerOn = false, deviceType = DeviceType.OTHER)
)

@Composable
fun IoTListSection(
    onDeviceClick: (Int) -> Unit,
    onMoreClick: () -> Unit
) {
    var devices by remember { mutableStateOf(mockDevices) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitleComponent(title = "기기", onMoreClick = onMoreClick)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            devices.chunked(2).forEach { rowDevices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowDevices.forEach { device ->
                        Box(modifier = Modifier.weight(1f)) {
                            DeviceListItem(
                                place = device.place,
                                name = device.name,
                                isOnline = device.isOnline,
                                isPowerOn = device.isPowerOn,
                                deviceType = device.deviceType,
                                onPowerToggle = {
                                    devices = devices.map {
                                        if (it.id == device.id) it.copy(isPowerOn = !it.isPowerOn) else it
                                    }
                                },
                                onClick = { onDeviceClick(device.id) }
                            )
                        }
                    }
//                    if (rowDevices.size < 2) {
//                        Spacer(modifier = Modifier.weight(1f))
//                    }
                }
            }
        }
    }
}
