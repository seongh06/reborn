package com.reborn.feature.admin.adjust.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AdminAdjustUiState {
    data object Loading : AdminAdjustUiState
    data object Adjust: AdminAdjustUiState
/*    data object Connected : AdminAdjustUiState
    data object Disconnected : AdminAdjustUiState*/
}

sealed interface AdminAdjustIntent{
    data object LoadInitial : AdminAdjustIntent
    data object NaviageBack : AdminAdjustIntent
    data object AddDevice : AdminAdjustIntent
}