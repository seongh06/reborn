package com.reborn.feature.intro.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.intro.IntroViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntroModeSelectScreen(
    onAerometerClick: () -> Unit,
    onAdminClick: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: IntroViewModel = koinViewModel()
) {
    Column(
        modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
    ) {
        RebornTopAppBar(onBackClick = { onBackClick() })

        Spacer(modifier = Modifier.weight(1f))

        RebornButton(
            text = "공기계",
            onClick = { onAerometerClick() }
        )
        RebornButton(
            text = "관리자",
            onClick = { onAdminClick() },
            backgroundColor = RebornTheme.color.grayScale400
        )
    }
}
