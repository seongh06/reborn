package com.reborn.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme

@Composable
fun RebornButton(
    modifier: Modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
    text: String,
    enabled: Boolean = true,
    round: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    textStyle: TextStyle = RebornTheme.typography.labelLarge,
    backgroundColor: Color = if (enabled) RebornTheme.color.grayScale100 else RebornTheme.color.grayScale600,
    contentColor: Color = if (enabled) RebornTheme.color.grayScale900 else RebornTheme.color.grayScale500,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor =backgroundColor,
            disabledContentColor = contentColor
        ),
        shape = RoundedCornerShape(round),
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = textStyle,
                color = contentColor
            )
        }
    }
}