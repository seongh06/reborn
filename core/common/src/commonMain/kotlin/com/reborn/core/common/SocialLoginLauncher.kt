package com.reborn.core.common

import androidx.compose.runtime.Composable

enum class SocialType {
    KAKAO,
    GOOGLE,
}

@Composable
expect fun rememberSocialLoginLauncher(
    onResult: (provider: String, token: String) -> Unit,
    onError: (Throwable) -> Unit,
): SocialLoginLauncher

interface SocialLoginLauncher {
    fun launch(socialType: SocialType)
}
