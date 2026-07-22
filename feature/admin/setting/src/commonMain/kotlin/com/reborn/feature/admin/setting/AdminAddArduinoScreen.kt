package com.reborn.feature.admin.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTextField
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.ext.rebornDefault
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminAddArduinoRoute(
    placeId: Long,
    viewModel: AdminAddArduinoViewModel = koinViewModel(),
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var deviceId by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is AdminAddArduinoEvent.RegisterSuccess -> onBackClick()
                is AdminAddArduinoEvent.ShowErrorSnackbar ->
                    snackbarHostState.showSnackbar(event.throwable.message ?: "기기 등록에 실패했습니다.")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        Column(
            modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
        ) {
            RebornTopAppBar(title = "아두이노 추가", onBackClick = onBackClick)

            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text(
                    "시리얼 번호",
                    style = RebornTheme.typography.titleMedium,
                    color = RebornTheme.color.grayScale900
                )
                RebornTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
                    value = deviceId,
                    onValueChange = { deviceId = it },
                    hint = "기기에 부착된 8자리 시리얼 번호 (예: AR7K2P9M)"
                )
                Text(
                    "기기 이름",
                    style = RebornTheme.typography.titleMedium,
                    color = RebornTheme.color.grayScale900
                )
                RebornTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    hint = "예: 거실"
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            RebornButton(
                text = "등록",
                enabled = deviceId.isNotBlank() && deviceName.isNotBlank() && uiState !is AdminAddArduinoUiState.Submitting,
                onClick = { viewModel.register(placeId, deviceId.trim(), deviceName.trim()) }
            )
        }
    }
}
