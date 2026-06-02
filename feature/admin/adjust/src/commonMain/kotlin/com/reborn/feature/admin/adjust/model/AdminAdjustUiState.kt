package com.reborn.feature.admin.adjust.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AdminAdjustUiState {

    data object Loading : AdminAdjustUiState

}

sealed interface AdminAdjustIntent{



}