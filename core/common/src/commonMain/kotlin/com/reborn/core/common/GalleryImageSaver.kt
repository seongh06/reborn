package com.reborn.core.common

expect class GalleryImageSaver {
    suspend fun saveFromUrl(url: String, displayName: String): Boolean
}
