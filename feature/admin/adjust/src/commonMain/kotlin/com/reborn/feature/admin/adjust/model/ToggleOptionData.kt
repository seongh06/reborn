package com.reborn.feature.admin.adjust.model

data class ToggleOptionData(
    val isChecked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
    val minutesValue: String,
    val onMinutesValueChange: (String) -> Unit
)