package com.reborn.feature.admin.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reborn.core.ui.component.DeviceListItem
import com.reborn.core.ui.component.DeviceType
import com.reborn.core.ui.component.SectionTitleComponent

data class IoTDeviceItem(
    val id: String,
    val place: String,
    val name: String,
    val isOnline: Boolean,
    val isPowerOn: Boolean,
    val deviceType: DeviceType = DeviceType.OTHER
)

@Composable
fun IoTListSection(
    devices: List<IoTDeviceItem>,
    onDeviceClick: (String) -> Unit,
    onPowerToggle: (String) -> Unit,
    onMoreClick: () -> Unit
) {
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
                                onPowerToggle = { onPowerToggle(device.id) },
                                onClick = { onDeviceClick(device.id) }
                            )
                        }
                    }
                    if (rowDevices.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
