package com.reborn.core.common

import androidx.compose.runtime.Composable

@Composable
expect fun rememberToast(): (String) -> Unit
