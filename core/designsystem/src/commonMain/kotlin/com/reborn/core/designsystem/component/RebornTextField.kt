package com.reborn.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import reborn.core.designsystem.generated.resources.Res
import reborn.core.designsystem.generated.resources.ic_check

@Composable
fun RebornTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String? = null,
    status: Int? = null,
    onDone: (() -> Unit)? = null,
    maxLine: Int = 1
) {
    val isNotEmpty = value.isNotEmpty()
    val keyboardController = LocalSoftwareKeyboardController.current

    val customTextSelectionColors = TextSelectionColors(
        handleColor = RebornTheme.color.grayScale900,
        backgroundColor = RebornTheme.color.grayScale900.copy(alpha = 0.4f)
    )

    val emptyTextToolbar = object : TextToolbar {
        override val status: TextToolbarStatus = TextToolbarStatus.Hidden
        override fun hide() {}
        override fun showMenu(
            rect: Rect,
            onCopyRequested: (() -> Unit)?,
            onPasteRequested: (() -> Unit)?,
            onCutRequested: (() -> Unit)?,
            onSelectAllRequested: (() -> Unit)?
        ) {}
    }

    CompositionLocalProvider(
        LocalTextSelectionColors provides customTextSelectionColors,
        LocalTextToolbar provides emptyTextToolbar
    ) {
        BasicTextField(
            modifier = modifier.fillMaxWidth(),
            value = value,
            onValueChange = {
                onValueChange(it)
            },
            maxLines = maxLine,
            singleLine = maxLine == 1,
            textStyle = RebornTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(
                imeAction = if (maxLine > 1) ImeAction.Default else ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    onDone?.invoke()
                }
            ),
            cursorBrush = SolidColor(RebornTheme.color.grayScale900),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = RebornTheme.color.grayScale100,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = if (maxLine > 1) Alignment.Top else Alignment.CenterVertically,
                    ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = if (maxLine > 1) Alignment.TopStart else Alignment.CenterStart
                    ) {
                        if (!isNotEmpty) {
                            hint?.let { h ->
                                Text(
                                    text = h,
                                    style = RebornTheme.typography.bodyLarge,
                                    color = RebornTheme.color.grayScale500
                                )
                            }
                        }
                        innerTextField()
                        if (isNotEmpty && status == 1) {
                            RebornIcon(
                                icon = Res.drawable.ic_check,
                                onClick = {}
                            )
                        }
                    }
                }
            }
        )
    }
}