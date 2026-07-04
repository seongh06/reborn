package com.reborn.feature.admin.data.component.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme

@Composable
fun AnalysisResultSection(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "분석 결과",
            style = RebornTheme.typography.headlineMedium,
            color = RebornTheme.color.grayScale900
        )
        Text(
            text = text,
            style = RebornTheme.typography.bodyMedium,
            color = RebornTheme.color.grayScale700
        )
    }
}
