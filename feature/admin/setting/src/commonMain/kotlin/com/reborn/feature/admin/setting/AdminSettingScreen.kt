package com.reborn.feature.admin.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.component.SettingItem
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.setting.component.RoomListItem
import com.reborn.feature.admin.setting.model.AdminSettingIntent
import com.reborn.feature.admin.setting.model.AdminSettingUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminSettingRoute(
    viewModel: AdminSettingViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onNavigateToInviteCode: (Int) -> Unit = {},
    onNavigateToAddDevice: (Int) -> Unit = {},
    onNavigateToAddArduino: (Int) -> Unit = {},
    onNavigateToAddPlace: () -> Unit = {},
    onLoggedOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onIntent(AdminSettingIntent.LoadInitial)

        viewModel.event.collect { event ->
            when (event) {
                is AdminSettingEvent.ShowErrorSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.throwable.message ?: "에러가 발생했습니다."
                    )
                }
                is AdminSettingEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
                is AdminSettingEvent.Exit -> onBackClick()
                is AdminSettingEvent.NavigateToInviteCode -> onNavigateToInviteCode(event.placeId)
                is AdminSettingEvent.NavigateToAddDevice -> onNavigateToAddDevice(event.placeId)
                is AdminSettingEvent.NavigateToAddArduino -> onNavigateToAddArduino(event.placeId)
                is AdminSettingEvent.NavigateToAddPlace -> onNavigateToAddPlace()
                is AdminSettingEvent.LoggedOut -> onLoggedOut()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        when (val state = uiState) {
            is AdminSettingUiState.Loading -> RebornLoadingScreen()
            is AdminSettingUiState.Setting -> AdminSettingScreen(
                state = state,
                onBackClick = onBackClick,
                onDeleteRoomClick = { placeId -> viewModel.onIntent(AdminSettingIntent.DeleteRoom(placeId)) },
                onAddAdminClick = { placeId -> viewModel.onIntent(AdminSettingIntent.ClickAddAdmin(placeId)) },
                onAddDeviceClick = { placeId -> viewModel.onIntent(AdminSettingIntent.ClickAddDevice(placeId)) },
                onAddArduinoClick = { placeId -> viewModel.onIntent(AdminSettingIntent.ClickAddArduino(placeId)) },
                onAddPlaceClick = { viewModel.onIntent(AdminSettingIntent.ClickAddPlace) },
                onLogoutClick = { viewModel.onIntent(AdminSettingIntent.ClickLogout) }
            )
        }
    }
}

@Composable
fun AdminSettingScreen(
    state: AdminSettingUiState.Setting,
    onBackClick: () -> Unit,
    onDeleteRoomClick: (Int) -> Unit,
    onAddAdminClick: (Int) -> Unit,
    onAddDeviceClick: (Int) -> Unit,
    onAddArduinoClick: (Int) -> Unit,
    onAddPlaceClick: () -> Unit,
    onLogoutClick: () -> Unit

) {
    Column(
        modifier = Modifier
            .rebornDefault(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        RebornTopAppBar(title = "설정", onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Place",
                style = RebornTheme.typography.titleMedium,
                color = RebornTheme.color.grayScale900
            )
            state.rooms.forEach { room ->
                RoomListItem(
                    placeId = room.placeId,
                    roomName = room.roomName,
                    adminCount = room.adminCount,
                    deviceCount = room.deviceCount,
                    onDeleteClick = { onDeleteRoomClick(room.placeId) },
                    onAddAdminClick = { onAddAdminClick(room.placeId) },
                    onAddDeviceClick = { onAddDeviceClick(room.placeId) },
                    onAddArduinoClick = { onAddArduinoClick(room.placeId) }
                )
            }
            RebornButton(
                modifier = Modifier.fillMaxWidth(),
                text = "새로운 place 추가",
                onClick = onAddPlaceClick
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "정보 및 기타",
                style = RebornTheme.typography.titleMedium,
                color = RebornTheme.color.grayScale900
            )
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(RebornTheme.color.grayScale100)
                    .border(
                        width = 1.dp,
                        color = RebornTheme.color.grayScale200,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                HorizontalDivider(color = RebornTheme.color.grayScale300)
                SettingItem(label = "서비스 소개", onClick = {})
                HorizontalDivider(color = RebornTheme.color.grayScale300)
                SettingItem(label = "이용약관", onClick = {})
                HorizontalDivider(color = RebornTheme.color.grayScale300)
                SettingItem(label = "로그아웃", onClick = onLogoutClick)
                HorizontalDivider(color = RebornTheme.color.grayScale300)
                SettingItem(label = "탈퇴", onClick = {})
            }
        }
    }
}
