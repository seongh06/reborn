package com.reborn.core.common

import androidx.compose.runtime.Composable

// 공기계 모드는 실제로는 안 쓰는 안드로이드 폰에 설치하는 용도라(iOS 배포 대상 아님) no-op로 둔다 -
// rememberToast()의 iOS 스텁과 동일한 패턴.
@Composable
actual fun KeepScreenOn() {
}
