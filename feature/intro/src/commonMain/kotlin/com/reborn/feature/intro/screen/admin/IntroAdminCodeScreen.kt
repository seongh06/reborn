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
import com.reborn.feature.intro.IntroViewModel.Companion.ADMIN_CODE_TTL_SECONDS
import com.reborn.feature.intro.component.PairingCodeIssued
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

// Setting의 "관리자 초대" 버튼에서 진입하는 관리자 초대 코드(#10) 화면.
// 실제 완료(초대받은 사람이 코드를 입력함)는 이 화면에서 알 수 없다 - 향후 서버 이벤트/소켓으로
// 실시간 알림을 받기 전까지는 뒤로가기로만 나갈 수 있다(#110).
@Composable
fun IntroAdminCodeScreen(
    placeId: Long,
    onBackClick: () -> Unit,
    viewModel: IntroViewModel = koinViewModel()
) {
    val waitTime = 60

    var code by remember { mutableStateOf<String?>(null) }
    var timeLeft by remember { mutableStateOf(ADMIN_CODE_TTL_SECONDS) }

    LaunchedEffect(Unit) {
        viewModel.generateAdminCode(placeId)

        viewModel.event.collect { event ->
            when (event) {
                is IntroEvent.AdminCodeIssued -> {
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
                is IntroEvent.PairingCodeIssued,
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

    val canReissue = (ADMIN_CODE_TTL_SECONDS - timeLeft) >= waitTime

    val issuedCode = code
    if (issuedCode == null) {
        RebornLoadingScreen()
        return
    }

    Column(
        modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
    ) {
        RebornTopAppBar(onBackClick = { onBackClick() })
        RebornTopAppBar(title = "관리자 초대 코드")
        PairingCodeIssued(issuedCode, timeLeft)
        Spacer(modifier = Modifier.weight(1f))

        RebornButton(
            text = "코드 재발급",
            enabled = canReissue,
            onClick = { viewModel.generateAdminCode(placeId) }
        )
    }
}
