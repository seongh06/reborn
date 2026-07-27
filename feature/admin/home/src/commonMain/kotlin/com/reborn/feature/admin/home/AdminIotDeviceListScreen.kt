package com.reborn.feature.admin.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.ui.component.DeviceListItem
import com.reborn.core.ui.component.DeviceType
import com.reborn.core.ui.component.SectionTitleComponent
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.home.component.IoTDeviceItem

// TODO: 서버 device API 연동 전까지의 목업 데이터. 실제 연동 시 UseCase로 대체 예정 - Figma 595:5471
// 방(room) 개념이 device/place 도메인 모델에 없어(#166), device.place 문자열로 클라이언트에서만 그룹핑
private val mockRoomDevices = listOf(
    IoTDeviceItem(1, "거실", "거실 조명", isOnline = true, isPowerOn = true, deviceType = DeviceType.LAMP),
    IoTDeviceItem(2, "거실", "거실 TV", isOnline = true, isPowerOn = false, deviceType = DeviceType.TV),
    IoTDeviceItem(3, "거실", "거실 공기청정기", isOnline = true, isPowerOn = false, deviceType = DeviceType.AIR_CONDITIONER),
    IoTDeviceItem(4, "거실", "거실 커튼", isOnline = false, isPowerOn = false, deviceType = DeviceType.CURTAIN),
    IoTDeviceItem(5, "안방", "안방 가습기", isOnline = false, isPowerOn = false, deviceType = DeviceType.OTHER),
    IoTDeviceItem(6, "안방", "안방 콘센트", isOnline = true, isPowerOn = true, deviceType = DeviceType.PLUG),
)

@Composable
fun AdminIotDeviceListRoute(
    onBackClick: () -> Unit
) {
    var devices by remember { mutableStateOf(mockRoomDevices) }

    AdminIotDeviceListScreen(
        devices = devices,
        onBackClick = onBackClick,
        onPowerToggle = { deviceId ->
            devices = devices.map {
                if (it.id == deviceId) it.copy(isPowerOn = !it.isPowerOn) else it
            }
        }
    )
}

@Composable
fun AdminIotDeviceListScreen(
    devices: List<IoTDeviceItem>,
    onBackClick: () -> Unit,
    onPowerToggle: (Int) -> Unit
) {
    val groupedDevices = devices.groupBy { it.place }

    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ) {
        RebornTopAppBar(title = "기기", onBackClick = onBackClick)

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            groupedDevices.forEach { (place, roomDevices) ->
                item(key = "room_$place") {
                    SectionTitleComponent(
                        title = place,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(roomDevices.chunked(2), key = { it.first().id }) { rowDevices ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowDevices.forEach { device ->
                            Box(modifier = Modifier.weight(1f)) {
                                DeviceListItem(
                                    place = device.place,
                                    name = device.name,
                                    isOnline = device.isOnline,
                                    isPowerOn = device.isPowerOn,
                                    deviceType = device.deviceType,
                                    onPowerToggle = { onPowerToggle(device.id) },
                                    onClick = {}
                                )
                            }
                        }
                        if (rowDevices.size < 2) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
