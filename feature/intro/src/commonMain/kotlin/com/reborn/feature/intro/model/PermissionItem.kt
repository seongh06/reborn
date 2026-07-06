package com.reborn.feature.intro.model

import com.reborn.core.common.PermissionType

data class PermissionItem(
    val id: Int,
    val type: PermissionType,
    val title: String,
    val content: String
)