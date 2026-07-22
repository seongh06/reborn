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
fun AdminAddAiSpeakerRoute(
    placeId: Long,
    viewModel: AdminAddAiSpeakerViewModel = koinViewModel(),
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var deviceId by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is AdminAddAiSpeakerEvent.RegisterSuccess -> onBackClick()
                is AdminAddAiSpeakerEvent.ShowErrorSnackbar ->
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
            RebornTopAppBar(title = "AI 스피커 추가", onBackClick = onBackClick)

            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text(
                    "기기 ID",
                    style = RebornTheme.typography.titleMedium,
                    color = RebornTheme.color.grayScale900
                )
                RebornTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
                    value = deviceId,
                    onValueChange = { deviceId = it },
                    hint = "기기 프로비저닝 시 입력한 기기 ID와 동일하게 (예: speaker_livingroom_01)"
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
                    hint = "예: 거실 스피커"
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            RebornButton(
                text = "등록",
                enabled = deviceId.isNotBlank() && deviceName.isNotBlank() && uiState !is AdminAddAiSpeakerUiState.Submitting,
                onClick = { viewModel.register(placeId, deviceId.trim(), deviceName.trim()) }
            )
        }
    }
}
