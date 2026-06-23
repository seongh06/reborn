package com.reborn.feature.intro.model

data class TermItem(
    val id: String,
    val title: String,
    val content: String,
    val isRequired: Boolean = true
)