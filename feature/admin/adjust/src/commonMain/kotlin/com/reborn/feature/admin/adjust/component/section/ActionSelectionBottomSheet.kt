package com.reborn.feature.admin.adjust.component.section

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSelectionBottomSheet(
    onDismissRequest: () -> Unit,
    actionOptions: List<String>,
    onOptionSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = RebornTheme.color.grayScale100
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .heightIn(max = 240.dp)
        ) {
            items(actionOptions) { option ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            onOptionSelected(option)
                            onDismissRequest()
                        }
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = option,
                        style = RebornTheme.typography.bodyMedium,
                        color = RebornTheme.color.grayScale900
                    )
                }
            }
        }
    }
}