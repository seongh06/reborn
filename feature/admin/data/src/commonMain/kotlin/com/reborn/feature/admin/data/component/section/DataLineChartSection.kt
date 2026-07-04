package com.reborn.feature.admin.data.component.section

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 4f

@Composable
fun DataLineChartSection(
    labels: List<String>,
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    val lineColor = RebornTheme.color.grayScale800
    val gridColor = RebornTheme.color.grayScale300

    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }

    fun clampOffset(currentScale: Float, currentOffset: Float): Float {
        val contentWidth = canvasWidthPx * currentScale
        val minOffset = (canvasWidthPx - contentWidth).coerceAtMost(0f)
        return currentOffset.coerceIn(minOffset, 0f)
    }

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
                .height(180.dp)
                .clipToBounds()
                .onSizeChanged { canvasWidthPx = it.width.toFloat() }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                        offsetX = clampOffset(newScale, offsetX + pan.x)
                        scale = newScale
                    }
                }
        ) {
            if (values.size < 2) return@Canvas

            val maxValue = values.max()
            val minValue = values.min()
            val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f

            val verticalPadding = 12f
            val drawableHeight = size.height - verticalPadding * 2
            val contentWidth = size.width * scale
            val stepX = contentWidth / (values.size - 1)

            val points = values.mapIndexed { index, value ->
                Offset(
                    x = offsetX + stepX * index,
                    y = verticalPadding + drawableHeight - ((value - minValue) / range) * drawableHeight
                )
            }

            val gridLineCount = 3
            for (i in 0..gridLineCount) {
                val y = verticalPadding + drawableHeight * i / gridLineCount
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val current = points[i]
                    val next = points[i + 1]
                    val midX = (current.x + next.x) / 2f
                    val midY = (current.y + next.y) / 2f
                    quadraticTo(current.x, current.y, midX, midY)
                }
                lineTo(points.last().x, points.last().y)
            }

            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(points.last().x, verticalPadding + drawableHeight)
                lineTo(points.first().x, verticalPadding + drawableHeight)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.2f), Color.Transparent)
                )
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            points.forEach { point ->
                drawCircle(color = Color.White, radius = 7f, center = point)
                drawCircle(color = lineColor, radius = 5f, center = point)
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
