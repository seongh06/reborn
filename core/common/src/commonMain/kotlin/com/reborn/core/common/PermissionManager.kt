package com.reborn.core.common

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPermissionManager(
    onPermissionResult: (PermissionType, Boolean) -> Unit
): PermissionHandler

enum class PermissionType { CAMERA, LOCATION, GALLERY, ALARM }

interface PermissionHandler {
    fun askPermission(type: PermissionType)
    fun isPermissionGranted(type: PermissionType): Boolean
}