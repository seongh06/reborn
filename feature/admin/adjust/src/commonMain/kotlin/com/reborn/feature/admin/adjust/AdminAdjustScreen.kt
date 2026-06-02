package com.reborn.feature.admin.adjust

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.feature.admin.adjust.model.AdminAdjustUiState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalUnsignedTypes::class)
@Composable
fun AdminAdjustRoute(
    viewModel: AdminAdjustViewModel = koinViewModel()
){

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminAdjustScreen(
        state = uiState
    )

}



@Composable
fun AdminAdjustScreen(
    state: AdminAdjustUiState
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(if (state is AdminAdjustUiState.Loading) "IoT Connected" else "IoT Disconnected")
    }
}
