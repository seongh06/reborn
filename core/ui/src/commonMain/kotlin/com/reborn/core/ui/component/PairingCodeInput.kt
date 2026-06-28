package com.reborn.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import kotlinx.coroutines.delay

@Composable
fun PairingCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxCount: Int = 6,
    isError: Boolean = false,
    onErrorReset: () -> Unit = {}
) {
    LaunchedEffect(isError) {
        if (isError) {
            delay(700)
            onValueChange("")
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                if (isError) onErrorReset()
                val filtered = newValue.filter { it.isDigit() }
                if (filtered.length <= maxCount) {
                    onValueChange(filtered)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = modifier,
            decorationBox = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(maxCount) { index ->
                        val char = value.getOrNull(index)?.toString() ?: ""
                        val isFocused = value.length == index

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(62.dp)
                                .background(
                                    color = if (char.isNotEmpty()) Color.Transparent else RebornTheme.color.grayScale400,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        isError -> RebornTheme.color.temperature
                                        isFocused -> RebornTheme.color.grayScale700
                                        char.isNotEmpty() -> RebornTheme.color.grayScale400
                                        else -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                style = RebornTheme.typography.displayLarge,
                                color = if (isError) RebornTheme.color.temperature else RebornTheme.color.grayScale900
                            )
                        }
                    }
                }
            }
        )
        Text(
            text = if (isError) "잘못된 코드입니다." else "관리자가 발급한 페어링 코드를 작성해주세요.",
            style = RebornTheme.typography.bodyMedium,
            color = if (isError) RebornTheme.color.temperature else RebornTheme.color.grayScale900
        )
    }
}