package com.reborn.core.common

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberToast(): (String) -> Unit {
    val context = LocalContext.current
    return { message ->
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
