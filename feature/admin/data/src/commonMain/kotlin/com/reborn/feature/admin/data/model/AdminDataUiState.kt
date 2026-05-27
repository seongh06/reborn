package com.reborn.feature.admin.data.model

data class AdminDataUiState(
    val isLoading: Boolean = false,
    val dataList: List<String> = emptyList()
)
