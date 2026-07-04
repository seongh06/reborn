package com.reborn.feature.admin.adjust.model

import com.reborn.core.ui.component.DeviceType

data class Device(
    val id: String,
    val name: String,
    val place: String,
    val isOnline: Boolean = false,
    val isPowerOn: Boolean = false,
    val deviceType: DeviceType
)