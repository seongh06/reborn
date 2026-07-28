package com.reborn.core.common

import androidx.compose.runtime.Composable

data class PickedImage(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
)

// 갤러리에서 이미지 1장을 선택해 바이트로 읽어온다(프로필 이미지 변경). 반환된 함수를 호출하면
// 선택 UI가 뜬다 - SocialLoginLauncher와 동일하게 launch를 별도 함수로 반환하는 패턴.
@Composable
expect fun rememberImagePicker(
    onImagePicked: (PickedImage) -> Unit,
    onError: (Throwable) -> Unit = {},
): () -> Unit
