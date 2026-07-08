package com.reborn.feature.intro.screen.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.reborn.core.common.SocialType
import com.reborn.core.common.rememberSocialLoginLauncher
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.intro.IntroEvent
import com.reborn.feature.intro.IntroViewModel
import com.reborn.feature.intro.component.SocialLoginButton
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntroAdminLoginScreen(
    onLoginClick: () -> Unit,
    viewModel: IntroViewModel = koinViewModel()
) {
    val socialLoginLauncher = rememberSocialLoginLauncher(
        onResult = { provider, token -> viewModel.login(provider, token) },
        onError = { viewModel.reportError(it) }
    )

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            if (event is IntroEvent.LoginSuccess) {
                onLoginClick()
            }
        }
    }

    Column(
        modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        SocialLoginButton(
            socialType = SocialType.KAKAO,
            onClick = { socialLoginLauncher.launch(SocialType.KAKAO) }
        )
        SocialLoginButton(
            socialType = SocialType.GOOGLE,
            onClick = { socialLoginLauncher.launch(SocialType.GOOGLE) }
        )
    }
}
