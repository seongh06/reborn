package com.reborn.feature.admin.adjust.model

import androidx.compose.ui.text.input.KeyboardType

data class RuleData(
    val id: String,
    val inputValue: String,
    val onInputValueChange: (String) -> Unit,
    val conditionLabel: String,
    val actionText: String,
    val onActionClick: () -> Unit,
    val keyboardType: KeyboardType = KeyboardType.Text
)