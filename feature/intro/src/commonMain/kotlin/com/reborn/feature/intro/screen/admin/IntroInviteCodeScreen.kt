package com.reborn.feature.intro.screen.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.component.PairingCodeInput
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.intro.IntroEvent
import com.reborn.feature.intro.IntroViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntroInviteCodeScreen(
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: IntroViewModel = koinViewModel()
) {
    var inviteCode by remember { mutableStateOf("") }
    // 서버 관리자 초대 코드는 8자리(A-Z0-9) - PlaceService.ADMIN_CODE_LENGTH와 동일
    val maxCount = 8

    var pairingCodeError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is IntroEvent.InviteCodeVerified -> onNextClick()
                is IntroEvent.InviteCodeInvalid -> pairingCodeError = true
                else -> {}
            }
        }
    }

    LaunchedEffect(inviteCode) {
        if (inviteCode.length == maxCount) {
            viewModel.verifyInviteCode(inviteCode)
        }
    }

    Column(
        modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
    ) {
        RebornTopAppBar(title = "초대 코드 입력", onBackClick = { onBackClick() })
        PairingCodeInput(
            value = inviteCode,
            onValueChange = {
                if (it.length <= maxCount) {
                    inviteCode = it
                }
            },
            maxCount = maxCount,
            isError = pairingCodeError,
            onErrorReset = { pairingCodeError = false }
        )
    }
}
