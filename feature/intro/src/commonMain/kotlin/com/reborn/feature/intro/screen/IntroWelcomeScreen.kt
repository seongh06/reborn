package com.reborn.feature.intro.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.reborn.core.common.SocialType
import com.reborn.core.common.rememberSocialLoginLauncher
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.intro.IntroEvent
import com.reborn.feature.intro.IntroViewModel
import com.reborn.feature.intro.component.SocialLoginButton
import org.koin.compose.viewmodel.koinViewModel

// 기존 Start(인트로)+Term(약관)+Permission(권한)+ModeSelect+AdminLogin 5개 화면을 하나로 통합한 Welcome 화면(#160)
@Composable
fun IntroWelcomeScreen(
    onLoginSuccess: (needsPlaceSetup: Boolean) -> Unit,
    onAerometerClick: () -> Unit,
    viewModel: IntroViewModel = koinViewModel()
) {
    val socialLoginLauncher = rememberSocialLoginLauncher(
        onResult = { provider, token -> viewModel.login(provider, token) },
        onError = { viewModel.reportError(it) }
    )

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            if (event is IntroEvent.LoginSuccess) {
                onLoginSuccess(event.needsPlaceSetup)
            }
        }
    }

    Column(
        modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
    ) {
        Text(
            text = "공기계로 시작하기",
            style = RebornTheme.typography.bodyLarge,
            color = RebornTheme.color.grayScale700,
            textDecoration = TextDecoration.Underline,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAerometerClick)
                .padding(16.dp, 8.dp)
        )

        Column(
            modifier = Modifier.padding(16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Welcome!",
                style = RebornTheme.typography.displayLarge,
                color = RebornTheme.color.grayScale900
            )
            Text(
                text = "낡은 가전에 새 숨을 불어넣어, 우리 공간의 온도·습도·조도를 스마트폰 하나로 관리하세요.",
                style = RebornTheme.typography.bodyLarge,
                color = RebornTheme.color.grayScale900
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            SocialLoginButton(
                socialType = SocialType.KAKAO,
                onClick = { socialLoginLauncher.launch(SocialType.KAKAO) }
            )
            SocialLoginButton(
                socialType = SocialType.GOOGLE,
                onClick = { socialLoginLauncher.launch(SocialType.GOOGLE) }
            )
            val baseStyle = SpanStyle(color = RebornTheme.color.grayScale500)
            val emphasisStyle = SpanStyle(color = RebornTheme.color.grayScale700, fontWeight = FontWeight.SemiBold)
            Text(
                text = buildAnnotatedString {
                    withStyle(baseStyle) { append("계속하면 Reborn의 ") }
                    withStyle(emphasisStyle) { append("소비자 약관") }
                    withStyle(baseStyle) { append(" 및 ") }
                    withStyle(emphasisStyle) { append("이용 정책") }
                    withStyle(baseStyle) { append("에 동의하고, ") }
                    withStyle(emphasisStyle) { append("개인정보 처리방침") }
                    withStyle(baseStyle) { append("을 확인하는 것으로 간주됩니다.") }
                },
                style = RebornTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp)
            )
        }
    }
}
