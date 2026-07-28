package com.reborn.core.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// TODO: iOS 이미지 선택 미구현 - SocialLoginLauncher.ios.kt/SensorAnalyzer.ios.kt와 동일하게 이
// 프로젝트는 iOS를 실제로 빌드/테스트하지 않는 상태라 스텁으로 둔다. 실제 iOS 지원 시
// PHPickerViewController로 구현.
@Composable
actual fun rememberImagePicker(
    onImagePicked: (PickedImage) -> Unit,
    onError: (Throwable) -> Unit,
): () -> Unit {
    return remember {
        { onError(UnsupportedOperationException("iOS 이미지 선택은 아직 준비 중입니다.")) }
    }
}
