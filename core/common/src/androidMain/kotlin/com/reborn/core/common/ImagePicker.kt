package com.reborn.core.common

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// 프로필 이미지는 원본 화질이 필요 없는데도 폰 카메라 사진이 보통 몇 MB라, nginx/Spring 업로드
// 용량 제한(10MB, #217)에 종종 걸렸음 - 업로드 전에 미리 줄여서 애초에 여유 있게 만든다.
private const val MAX_UPLOAD_BYTES = 1_500_000L
private const val MAX_DIMENSION_PX = 1920

@Composable
actual fun rememberImagePicker(
    onImagePicked: (PickedImage) -> Unit,
    onError: (Throwable) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val originalFile = File.createTempFile("picked_", ".tmp", context.cacheDir)
                    try {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            originalFile.outputStream().use { output -> input.copyTo(output) }
                        } ?: error("이미지를 읽을 수 없습니다.")

                        // id.zelory:compressor - 해상도 상한 + 목표 용량 이하가 될 때까지 품질을
                        // 단계적으로 낮추는 size() 제약. 포맷은 항상 JPEG로 통일(입력이 PNG여도
                        // 프로필 사진에 투명도가 필요 없어 무관, 서버 허용 포맷에도 포함됨).
                        val compressedFile = Compressor.compress(context, originalFile) {
                            resolution(MAX_DIMENSION_PX, MAX_DIMENSION_PX)
                            quality(85)
                            format(Bitmap.CompressFormat.JPEG)
                            size(MAX_UPLOAD_BYTES)
                        }
                        val bytes = compressedFile.readBytes()
                        compressedFile.delete()
                        PickedImage(bytes = bytes, fileName = "profile.jpg", mimeType = "image/jpeg")
                    } finally {
                        originalFile.delete()
                    }
                }
            }.onSuccess(onImagePicked).onFailure(onError)
        }
    }

    return remember {
        { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }
}
