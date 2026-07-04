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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import kotlin.math.roundToInt

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
    val axisTextColor = RebornTheme.color.grayScale500
    val axisTextStyle = RebornTheme.typography.caption

    val textMeasurer = rememberTextMeasurer()
    val labelAreaWidthPx = with(LocalDensity.current) { 36.dp.toPx() }

    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var plotWidthPx by remember { mutableFloatStateOf(0f) }

    fun clampOffset(currentScale: Float, currentOffset: Float): Float {
        val contentWidth = plotWidthPx * currentScale
        val minOffset = (plotWidthPx - contentWidth).coerceAtMost(0f)
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
                .onSizeChanged { plotWidthPx = (it.width.toFloat() - labelAreaWidthPx).coerceAtLeast(0f) }
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
            val plotWidth = (size.width - labelAreaWidthPx).coerceAtLeast(0f)
            val contentWidth = plotWidth * scale
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
                    end = Offset(plotWidth, y),
                    strokeWidth = 1f
                )

                val axisValue = maxValue - range * i / gridLineCount
                val measuredText = textMeasurer.measure(
                    text = axisValue.roundToInt().toString(),
                    style = axisTextStyle.copy(color = axisTextColor)
                )
                drawText(
                    textLayoutResult = measuredText,
                    topLeft = Offset(plotWidth + 4f, y - measuredText.size.height / 2f)
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
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            sampledLabels(labels).forEach { label ->
                Text(
                    text = label,
                    style = RebornTheme.typography.caption,
                    color = RebornTheme.color.grayScale500
                )
            }
        }
    }
}

// 라벨 개수가 많을 때(1시간 간격=24개 등) 하단 라벨이 겹치지 않도록 대표 라벨만 골라서 표시
private fun sampledLabels(labels: List<String>, maxCount: Int = 6): List<String> {
    if (labels.size <= maxCount) return labels
    val step = (labels.size - 1).toFloat() / (maxCount - 1)
    return List(maxCount) { index -> labels[(index * step).toInt()] }
}
