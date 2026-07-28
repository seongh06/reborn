package com.reborn.core.common

// TODO: iOS 갤러리 저장 미구현 - SensorAnalyzer.ios.kt와 동일하게 이 프로젝트는 iOS를 실제로
// 빌드/테스트하지 않는 상태라 스텁으로 둔다. 실제 iOS 지원 시 UIImageWriteToSavedPhotosAlbum로 구현.
actual class GalleryImageSaver {
    actual suspend fun saveFromUrl(url: String, displayName: String): Boolean = false
}
