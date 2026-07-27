package com.reborn.feature.admin.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTextField
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.ext.rebornDefault
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminSmartThingsAddRoute(
    viewModel: AdminSmartThingsAddViewModel = koinViewModel(),
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is AdminSmartThingsAddEvent.OpenUrl -> uriHandler.openUri(event.url)
                is AdminSmartThingsAddEvent.ShowErrorSnackbar ->
                    snackbarHostState.showSnackbar(event.throwable.message ?: "오류가 발생했습니다.")
                is AdminSmartThingsAddEvent.RegisterSuccess -> {
                    snackbarHostState.showSnackbar("기기를 등록했습니다.")
                    onBackClick()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        if (uiState is AdminSmartThingsAddUiState.Loading) {
            RebornLoadingScreen()
        } else {
            Column(
                modifier = Modifier.rebornDefault(Color.White)
            ) {
                RebornTopAppBar(title = "SmartThings 기기 추가", onBackClick = onBackClick)
                when (val state = uiState) {
                    is AdminSmartThingsAddUiState.Idle -> IdleContent(onStartClick = viewModel::startAuthorize)
                    is AdminSmartThingsAddUiState.AwaitingConsent -> AwaitingConsentContent(
                        onRetryClick = viewModel::startAuthorize,
                        onLoadDevicesClick = viewModel::loadDevices
                    )
                    is AdminSmartThingsAddUiState.DeviceList -> DeviceListContent(
                        devices = state.devices,
                        onDeviceClick = viewModel::selectDevice
                    )
                    is AdminSmartThingsAddUiState.DeviceNaming -> DeviceNamingContent(
                        device = state.device,
                        name = state.name,
                        onNameChange = viewModel::updateDeviceName,
                        onBackClick = viewModel::backToDeviceList,
                        onRegisterClick = viewModel::registerDevice
                    )
                    is AdminSmartThingsAddUiState.Loading -> Unit
                }
            }
        }
    }
}

@Composable
private fun IdleContent(onStartClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "SmartThings 계정을 연동하면 에어컨 등 SmartThings로 등록된 기기를 이 장소에서 바로 제어할 수 있어요.",
            style = RebornTheme.typography.bodyLarge,
            color = RebornTheme.color.grayScale900
        )
        RebornButton(text = "SmartThings 계정 연동하기", onClick = onStartClick)
    }
}

@Composable
private fun AwaitingConsentContent(
    onRetryClick: () -> Unit,
    onLoadDevicesClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "브라우저에서 SmartThings 계정 연동을 완료한 뒤 이 화면으로 돌아와서 아래 버튼을 눌러주세요.",
            style = RebornTheme.typography.bodyLarge,
            color = RebornTheme.color.grayScale900
        )
        RebornButton(text = "기기 불러오기", onClick = onLoadDevicesClick)
        RebornButton(
            text = "연동 다시 시작하기",
            backgroundColor = RebornTheme.color.grayScale300,
            onClick = onRetryClick
        )
    }
}

@Composable
private fun DeviceListContent(
    devices: List<SmartThingsDeviceItem>,
    onDeviceClick: (SmartThingsDeviceItem) -> Unit
) {
    if (devices.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        ) {
            Text(
                "연동된 계정에서 등록 가능한 기기를 찾지 못했어요.",
                style = RebornTheme.typography.bodyLarge,
                color = RebornTheme.color.grayScale700
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = devices, key = { it.deviceId }) { device ->
            Text(
                text = device.label,
                style = RebornTheme.typography.titleSmall,
                color = RebornTheme.color.grayScale900,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RebornTheme.color.grayScale100)
                    .clickable { onDeviceClick(device) }
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun DeviceNamingContent(
    device: SmartThingsDeviceItem,
    name: String,
    onNameChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "기기 이름",
            style = RebornTheme.typography.titleSmall,
            color = RebornTheme.color.grayScale900
        )
        RebornTextField(
            value = name,
            onValueChange = onNameChange,
            hint = "예: 거실 에어컨",
            maxLength = 30
        )
        Spacer(modifier = Modifier.fillMaxWidth())
        RebornButton(
            text = "등록",
            enabled = name.isNotBlank(),
            onClick = onRegisterClick
        )
        RebornButton(
            text = "다른 기기 선택",
            backgroundColor = RebornTheme.color.grayScale300,
            onClick = onBackClick
        )
    }
}
