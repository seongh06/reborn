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
import com.reborn.feature.intro.IntroViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntroInviteCodeScreen(
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: IntroViewModel = koinViewModel()
) {
    var inviteCode by remember { mutableStateOf("") }
    val maxCount = 6

    var pairingCodeError by remember { mutableStateOf(false) }

    LaunchedEffect(inviteCode) {
        if (inviteCode.length == maxCount) {
            // TODO: viewModel.onIntent(IntroIntent.VerifyInviteCode(inviteCode)) 호출
            // 임시
            if (inviteCode == "123456") {
                onNextClick()
            } else {
                pairingCodeError = true
            }
        }
    }

    Column(
        modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
    ) {
        RebornTopAppBar(onBackClick = { onBackClick() })
        RebornTopAppBar(title = "초대 코드 입력")
        PairingCodeInput(
            value = inviteCode,
            onValueChange = {
                if (it.length <= maxCount) {
                    inviteCode = it
                }
            },
            maxCount = maxCount,
            isError = pairingCodeError
        )
    }
}
