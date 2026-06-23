package com.reborn.core.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat


@Composable
actual fun rememberPermissionManager(
    onPermissionResult: (PermissionType, Boolean) -> Unit
): PermissionHandler {
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult(PermissionType.CAMERA, isGranted)
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        onPermissionResult(PermissionType.LOCATION, granted)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult(PermissionType.GALLERY, isGranted)
    }

    val alarmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){ isGranted ->
        onPermissionResult(PermissionType.ALARM, isGranted)
    }

    return remember {
        object : PermissionHandler {
            override fun askPermission(type: PermissionType) {

                if (isPermissionGranted(type)) {
                    onPermissionResult(type, true)
                    return
                }

                when (type) {
                    PermissionType.CAMERA -> {
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                    PermissionType.LOCATION -> {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                    PermissionType.GALLERY -> {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_IMAGES
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        galleryLauncher.launch(permission)
                    }
                    PermissionType.ALARM -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            alarmLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onPermissionResult(PermissionType.ALARM, true)
                        }
                    }
                }
            }

            override fun isPermissionGranted(type: PermissionType): Boolean {
                return when (type) {
                    PermissionType.CAMERA -> {
                        context.hasPermission(Manifest.permission.CAMERA)
                    }
                    PermissionType.LOCATION -> {
                        context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                                context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                    PermissionType.GALLERY -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            context.hasPermission(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            context.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                    PermissionType.ALARM -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            true
                        }
                    }
                }
            }
        }
    }
}

private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}