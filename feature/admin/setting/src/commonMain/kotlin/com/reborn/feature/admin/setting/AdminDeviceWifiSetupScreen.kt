package com.reborn.feature.admin.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTextField
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.component.RebornScaffold
import com.reborn.core.ui.ext.rebornDefault
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminDeviceWifiSetupRoute(
    deviceId: String,
    placeId: Long,
    viewModel: AdminDeviceWifiSetupViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    // WiFi 설정 전송 성공 후 이동할 곳(#309) - 이전엔 onBackClick()을 그대로 재사용해서 기기
    // 등록 폼 화면(AddArduino/AddAiSpeaker)으로 되돌아갔음. 아두이노/AI 스피커 연결이 끝나면
    // 항상 홈 화면으로 보내야 해서 별도 콜백으로 분리 - 상단바 뒤로가기(onBackClick)는 그대로 취소 동작 유지.
    onWifiConfigured: () -> Unit = onBackClick,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadSavedWifi(placeId)
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is AdminDeviceWifiSetupEvent.ConfigureSuccess -> {
                    snackbarHostState.showSnackbar("설정을 전송했습니다. 기기가 재부팅되며 잠시 후 홈 WiFi로 연결됩니다.")
                    onWifiConfigured()
                }
                is AdminDeviceWifiSetupEvent.ShowErrorSnackbar ->
                    // 타임아웃/연결거부 등 Ktor 예외 메시지는 사용자에게 의미 없는 기술적 문구라
                    // 이 화면에서만은 항상 안내 문구로 통일한다(다른 화면과 달리 예외 message를 그대로 노출하지 않음)
                    snackbarHostState.showSnackbar(
                        "설정 전송에 실패했습니다. 기기의 WiFi(ReBorn-Setup-…)에 연결돼 있는지 확인해주세요."
                    )
                is AdminDeviceWifiSetupEvent.SavedWifiLoaded -> {
                    // 이미 사용자가 뭔가 입력해둔 상태라면(자동 채움이 늦게 도착) 덮어쓰지 않는다
                    if (ssid.isBlank() && event.ssid != null) ssid = event.ssid
                    if (password.isBlank() && event.password != null) password = event.password
                }
            }
        }
    }

    RebornScaffold(
        snackbarHostState = snackbarHostState
    ) { _ ->
        Column(
            modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
        ) {
            RebornTopAppBar(title = "기기 WiFi 설정", onBackClick = onBackClick)

            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RebornTheme.color.grayScale100)
                        .padding(16.dp)
                ) {
                    Text(
                        "1. 휴대폰 설정 > WiFi에서 \"ReBorn-Setup-\"으로 시작하는 네트워크에 연결해주세요.",
                        style = RebornTheme.typography.bodyMedium,
                        color = RebornTheme.color.grayScale900
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "2. 연결되면 이 화면으로 돌아와서, 기기가 실제로 사용할 홈 WiFi 정보를 아래에 입력해주세요.",
                        style = RebornTheme.typography.bodyMedium,
                        color = RebornTheme.color.grayScale900
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "설정할 기기",
                    style = RebornTheme.typography.labelMedium,
                    color = RebornTheme.color.grayScale700
                )
                Text(
                    deviceId,
                    style = RebornTheme.typography.titleMedium,
                    color = RebornTheme.color.grayScale900,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                Text(
                    "홈 WiFi 이름 (2.4GHz만 지원)",
                    style = RebornTheme.typography.titleMedium,
                    color = RebornTheme.color.grayScale900
                )
                RebornTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
                    value = ssid,
                    onValueChange = { ssid = it },
                    hint = "예: MyHomeWiFi"
                )
                Text(
                    "홈 WiFi 비밀번호",
                    style = RebornTheme.typography.titleMedium,
                    color = RebornTheme.color.grayScale900
                )
                RebornTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    value = password,
                    onValueChange = { password = it },
                    hint = "비밀번호 없으면 비워두세요"
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            RebornButton(
                text = "설정 전송",
                enabled = ssid.isNotBlank() && uiState !is AdminDeviceWifiSetupUiState.Submitting,
                onClick = { viewModel.configure(ssid.trim(), password, deviceId, placeId) }
            )
        }
    }
}
