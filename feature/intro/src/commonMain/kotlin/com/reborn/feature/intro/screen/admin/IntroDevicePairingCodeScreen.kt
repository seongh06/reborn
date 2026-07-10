package com.reborn.feature.intro.screen.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.intro.IntroEvent
import com.reborn.feature.intro.IntroViewModel
import com.reborn.feature.intro.IntroViewModel.Companion.PAIRING_CODE_TTL_SECONDS
import com.reborn.feature.intro.component.PairingCodeIssued
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

// 장소 생성 직후 공기계 기기를 이 장소에 연결하기 위한 페어링 코드(#08) 발급 화면.
// Setting의 "관리자 초대"에서 진입하는 IntroAdminCodeScreen(관리자 초대 코드, #10)과는 다른 화면 - #110
@Composable
fun IntroDevicePairingCodeScreen(
    placeId: Long?,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    viewModel: IntroViewModel = koinViewModel()
) {
    val waitTime = 60

    var code by remember { mutableStateOf<String?>(null) }
    var timeLeft by remember { mutableStateOf(PAIRING_CODE_TTL_SECONDS) }

    if (placeId == null) {
        // 정상 흐름에서는 발생하지 않음 - 온보딩 중 방금 등록한 장소로 항상 채워짐
        LaunchedEffect(Unit) { onBackClick() }
        return
    }

    LaunchedEffect(Unit) {
        viewModel.generatePairingCode(placeId)

        viewModel.event.collect { event ->
            when (event) {
                is IntroEvent.PairingCodeIssued -> {
                    code = event.code
                    timeLeft = event.remainingSeconds
                }
                // 코드 발급 실패 시 로딩 화면에 갇히지 않도록 이전 화면으로 돌아간다(에러 자체는 전역 스낵바가 표시)
                is IntroEvent.ShowErrorSnackbar -> onBackClick()
                is IntroEvent.NavigateToAdmin,
                is IntroEvent.NavigateToAerometer,
                is IntroEvent.PermissionGranted,
                is IntroEvent.ExitIntro,
                is IntroEvent.LoginSuccess,
                is IntroEvent.PlaceRegistered,
                is IntroEvent.AdminCodeIssued,
                is IntroEvent.InviteCodeVerified,
                is IntroEvent.InviteCodeInvalid,
                is IntroEvent.DevicePaired -> {}
            }
        }
    }

    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft -= 1
        }
    }

    val canReissue = (PAIRING_CODE_TTL_SECONDS - timeLeft) >= waitTime

    val issuedCode = code
    if (issuedCode == null) {
        RebornLoadingScreen()
        return
    }

    Column(
        modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
    ) {
        RebornTopAppBar(onBackClick = { onBackClick() })
        RebornTopAppBar(title = "공기계 페어링")
        PairingCodeIssued(issuedCode, timeLeft)
        Spacer(modifier = Modifier.weight(1f))

        RebornButton(
            text = "페어링 코드 재발급",
            enabled = canReissue,
            onClick = { viewModel.generatePairingCode(placeId) }
        )
        RebornButton(
            text = "완료",
            onClick = { onNextClick() }
        )
    }
}
