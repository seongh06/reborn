package com.reborn.feature.intro.screen.aerometer

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
import com.reborn.core.ui.component.PairingCodeInput
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.intro.IntroViewModel
import com.reborn.feature.intro.component.PairingCodeIssued
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntroAermeterPairingScreen(
    onPairingComplete: () -> Unit,
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
                onPairingComplete()
            } else {
                pairingCodeError = true
            }
        }
    }

    Column(
        modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
    ) {
        RebornTopAppBar(title = "페어링", onBackClick = { onBackClick() })
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
