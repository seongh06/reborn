package com.reborn.core.common

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

actual class GalleryImageSaver(private val context: Context) {

    actual suspend fun saveFromUrl(url: String, displayName: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = URL(url).openStream().use { it.readBytes() }
            saveJpegBytes(bytes, displayName)
        }.getOrDefault(false)
    }

    private fun saveJpegBytes(bytes: ByteArray, displayName: String): Boolean {
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ReBorn")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            val written = resolver.openOutputStream(uri)?.use { it.write(bytes) } != null
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            written
        } else {
            // API 28은 MediaStore의 IS_PENDING 플래그를 지원하지 않아 레거시 헬퍼로 대체
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return false
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.insertImage(resolver, bitmap, displayName, null) != null
        }
    }
}
