package com.reborn.feature.intro.screen.aerometer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTextField
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.intro.IntroEvent
import com.reborn.feature.intro.IntroViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntroAerometerDeviceNameScreen(
    onNextClick: () -> Unit,
    onBackClick:() -> Unit,
    viewModel: IntroViewModel = koinViewModel()
) {
    var deviceName by remember { mutableStateOf("") }
    var isPairing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is IntroEvent.DevicePaired -> onNextClick()
                is IntroEvent.ShowErrorSnackbar -> isPairing = false
                is IntroEvent.NavigateToAdmin,
                is IntroEvent.NavigateToAerometer,
                is IntroEvent.PermissionGranted,
                is IntroEvent.ExitIntro,
                is IntroEvent.LoginSuccess,
                is IntroEvent.PlaceRegistered,
                is IntroEvent.AdminCodeIssued,
                is IntroEvent.PairingCodeIssued,
                is IntroEvent.InviteCodeVerified,
                is IntroEvent.InviteCodeInvalid -> {}
            }
        }
    }

    if (isPairing) {
        RebornLoadingScreen()
        return
    }

    Column(
        modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
    ) {
        RebornTopAppBar(title = "기기 이름 입력", onBackClick = { onBackClick() })
        RebornTextField(
            modifier = Modifier.fillMaxWidth().padding(16.dp,8.dp),
            value = deviceName,
            onValueChange = { deviceName = it },
            hint = "기기 이름 입력"
        )
        Spacer(modifier = Modifier.weight(1f))

        RebornButton(
            text = "시작하기",
            enabled = deviceName.isNotBlank(),
            onClick = {
                isPairing = true
                viewModel.pairDevice(deviceName)
            }
        )
    }
}
