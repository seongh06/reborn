package com.reborn.feature.admin.adjust

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTextField
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.component.Dashboard
import com.reborn.core.ui.component.DeviceListItem
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.adjust.model.AdminAdjustIntent
import com.reborn.feature.admin.adjust.model.AdminAdjustUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminAdjustRoute(
    viewModel: AdminAdjustViewModel = koinViewModel(),
    onBackClick: () -> Unit,
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onIntent(AdminAdjustIntent.LoadInitial)

        viewModel.event.collect { event ->
            when (event) {
                is AdminAdjustEvent.ShowErrorSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.throwable.message ?: "에러가 발생했습니다."
                    )
                }
                is AdminAdjustEvent.Exit -> onBackClick()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState)}
    ){_ ->
        when(val state = uiState) {
            is AdminAdjustUiState.Loading -> RebornLoadingScreen()
            is AdminAdjustUiState.Adjust -> AdminAdjustScreen(
                state = state,
                onAddDeviceClick = { viewModel.onIntent(AdminAdjustIntent.NavigateToAddDevice) },
                onPowerToggle = { id -> viewModel.onIntent(AdminAdjustIntent.TogglePower(id)) }
            )
            is AdminAdjustUiState.AddDevice -> AdminAddDeviceScreen(
                onBackClick = { viewModel.onIntent(AdminAdjustIntent.NavigateBack) },
                onSubmit = { place, name -> viewModel.onIntent(AdminAdjustIntent.AddDevice(place, name)) }
            )
        }
    }
}

@Composable
fun AdminAdjustScreen(
    state: AdminAdjustUiState.Adjust,
    onAddDeviceClick: () -> Unit,
    onPowerToggle: (Int) -> Unit
) {
    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ){
        RebornTopAppBar(title = "기기 제어", onNavigateAddDevice = onAddDeviceClick)
        Dashboard("거실",20,20,20,20)
        Text(
            "연결된 기기",
            modifier = Modifier.padding(16.dp),
            style = RebornTheme.typography.titleSmall,
            color = RebornTheme.color.grayScale900
        )
        if (state.devices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "연결된 기기가 없습니다",
                    style = RebornTheme.typography.bodyMedium,
                    color = RebornTheme.color.grayScale500
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = state.devices, key = { it.id }) { device ->
                    DeviceListItem(
                        place = device.place,
                        name = device.name,
                        isOnline = device.isOnline,
                        isPowerOn = device.isPowerOn,
                        onPowerToggle = { onPowerToggle(device.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminAddDeviceScreen(
    onBackClick: () -> Unit,
    onSubmit: (place: String, name: String) -> Unit
) {
    var place by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ) {
        RebornTopAppBar(title = "기기 연결 추가", onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("장소", style = RebornTheme.typography.labelMedium, color = RebornTheme.color.grayScale700)
                RebornTextField(value = place, onValueChange = { place = it }, hint = "예) 거실")
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("기기 이름", style = RebornTheme.typography.labelMedium, color = RebornTheme.color.grayScale700)
                RebornTextField(value = name, onValueChange = { name = it }, hint = "예) 거실 조명")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        RebornButton(
            text = "추가하기",
            enabled = place.isNotBlank() && name.isNotBlank(),
            onClick = { onSubmit(place.trim(), name.trim()) }
        )
    }
}
