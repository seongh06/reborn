package com.reborn.feature.intro.screen.aerometer

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

// 공기계 앱에서 관리자가 발급한 페어링 코드(#08)를 입력하는 화면. 코드 단독 검증 API가 없어(#09는
// pairingCode+deviceName을 함께 받음) 여기서는 코드만 보관하고, 실제 API 호출은 다음 화면(기기 이름 입력)에서 한다.
@Composable
fun IntroAermeterPairingScreen(
    onPairingComplete: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: IntroViewModel = koinViewModel()
) {
    var inviteCode by remember { mutableStateOf("") }
    val maxCount = 6

    LaunchedEffect(inviteCode) {
        if (inviteCode.length == maxCount) {
            viewModel.setPairingCode(inviteCode)
            onPairingComplete()
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
            maxCount = maxCount
        )
    }
}
