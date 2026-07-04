package com.reborn.feature.admin.adjust

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.component.DataType
import com.reborn.core.ui.component.DeviceType
import com.reborn.core.ui.component.SensorChip
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.adjust.component.DeviceSection
import com.reborn.feature.admin.adjust.model.AdminAdjustUiState
import com.reborn.feature.admin.adjust.model.Device

@Composable
fun AdminDeviceDetailScreen(
    state: AdminAdjustUiState.DeviceDetail,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ) {
        RebornTopAppBar(title = "IoT 기기 상세보기", onBackClick = onBackClick)
        DeviceSection(
            Device(
                "1",
                "name",
                "place",
                true,
                true,
                DeviceType.AIR_CONDITIONER
            )
        )
        Column(
            modifier = Modifier.padding(12.dp, 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "현재 센서 상태",
                style = RebornTheme.typography.titleSmall,
                color = RebornTheme.color.grayScale900
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ){
                SensorChip(type = DataType.Temperature, value = 12)
                SensorChip(type = DataType.Humidity, value = 12)
                SensorChip(type = DataType.Illuminance, value = 12)
                SensorChip(type = DataType.PeopleCount, value = 12)
            }
        }
    }
}