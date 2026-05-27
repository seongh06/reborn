package com.reborn.feature.admin.adjust

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminAdjustScreen(
    viewModel: AdminAdjustViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(if (uiState.isDeviceConnected) "IoT Connected" else "IoT Disconnected")
    }
}
