package com.reborn.feature.admin.data.component.section

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme

@Composable
fun DataLineChartSection(
    labels: List<String>,
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    val lineColor = RebornTheme.color.grayScale800

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RebornTheme.color.grayScale100)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            if (values.size < 2) return@Canvas

            val maxValue = values.max()
            val minValue = values.min()
            val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
            val stepX = size.width / (values.size - 1)

            val points = values.mapIndexed { index, value ->
                Offset(
                    x = stepX * index,
                    y = size.height - ((value - minValue) / range) * size.height
                )
            }

            for (i in 0 until points.size - 1) {
                drawLine(
                    color = lineColor,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 4f
                )
            }
            points.forEach { point ->
                drawCircle(color = lineColor, radius = 6f, center = point)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = RebornTheme.typography.caption,
                    color = RebornTheme.color.grayScale500
                )
            }
        }
    }
}
