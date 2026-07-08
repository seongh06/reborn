package com.reborn.core.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberSocialLoginLauncher(
    onResult: (provider: String, token: String) -> Unit,
    onError: (Throwable) -> Unit,
): SocialLoginLauncher {
    return remember {
        object : SocialLoginLauncher {
            override fun launch(socialType: SocialType) {
                onError(UnsupportedOperationException("iOS 소셜 로그인은 아직 준비 중입니다."))
            }
        }
    }
}
