package com.reborn.feature.admin.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.component.DeviceListItem
import com.reborn.core.ui.component.RebornScaffold
import com.reborn.core.ui.component.SectionTitleComponent
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.home.component.IoTDeviceItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminIotDeviceListRoute(
    viewModel: AdminIotDeviceListViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onDeviceClick: (String) -> Unit = {},
    onAddDeviceClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadDevices()

        viewModel.event.collect { event ->
            when (event) {
                is AdminIotDeviceListEvent.ShowErrorSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.throwable.message ?: "에러가 발생했습니다.")
                }
            }
        }
    }

    RebornScaffold(
        snackbarHostState = snackbarHostState
    ) { _ ->
        when (val state = uiState) {
            is AdminIotDeviceListUiState.Loading -> RebornLoadingScreen()
            is AdminIotDeviceListUiState.Loaded -> AdminIotDeviceListScreen(
                devices = state.devices,
                onBackClick = onBackClick,
                onAddDeviceClick = onAddDeviceClick,
                onPowerToggle = { deviceId -> viewModel.togglePower(deviceId) },
                onDeviceClick = onDeviceClick
            )
        }
    }
}

@Composable
fun AdminIotDeviceListScreen(
    devices: List<IoTDeviceItem>,
    onBackClick: () -> Unit,
    onPowerToggle: (String) -> Unit,
    onDeviceClick: (String) -> Unit = {},
    onAddDeviceClick: () -> Unit = {}
) {
    val groupedDevices = devices.groupBy { it.place }

    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ) {
        RebornTopAppBar(title = "기기", onBackClick = onBackClick, onNavigateAddDevice = onAddDeviceClick)

        if (devices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "연결된 기기가 없습니다",
                    style = RebornTheme.typography.bodyMedium,
                    color = RebornTheme.color.grayScale500
                )
            }
        } else {
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
                                        onClick = { onDeviceClick(device.id) }
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
}
