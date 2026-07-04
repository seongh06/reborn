package com.reborn.feature.admin.data.component.section

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
    val axisTextStyle = RebornTheme.typography.caption.copy(color = RebornTheme.color.grayScale500)

    val textMeasurer = rememberTextMeasurer()
    val rightAxisWidthPx = with(LocalDensity.current) { 36.dp.toPx() }

    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var plotWidthPx by remember { mutableFloatStateOf(0f) }

    fun clampOffset(currentScale: Float, currentOffset: Float): Float {
        val contentWidth = plotWidthPx * currentScale
        val minOffset = (plotWidthPx - contentWidth).coerceAtMost(0f)
        return currentOffset.coerceIn(minOffset, 0f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(RebornTheme.color.grayScale100)
            .padding(12.dp)
            .clipToBounds()
            .onSizeChanged { plotWidthPx = (it.width.toFloat() - rightAxisWidthPx).coerceAtLeast(0f) }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                    offsetX = clampOffset(newScale, offsetX + pan.x)
                    scale = newScale
                }
            }
    ) {
        if (values.size < 2) return@Canvas

        val topPadding = 12f
        val bottomAxisHeight = 20f
        val plotWidth = (size.width - rightAxisWidthPx).coerceAtLeast(0f)
        val drawableHeight = size.height - topPadding - bottomAxisHeight

        val contentWidth = plotWidth * scale
        val stepX = contentWidth / (values.size - 1)
        val pointsX = values.indices.map { index -> offsetX + stepX * index }

        // 화면에 실제로 보이는 구간만으로 Y축 범위를 다시 계산 (확대/이동하면 값도 그 구간 기준으로 갱신)
        val visibleValues = values.filterIndexed { index, _ ->
            pointsX[index] in -stepX..(plotWidth + stepX)
        }.ifEmpty { values }
        val maxValue = visibleValues.max()
        val minValue = visibleValues.min()
        val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f

        val points = values.mapIndexed { index, value ->
            Offset(
                x = pointsX[index],
                y = topPadding + drawableHeight - ((value - minValue) / range) * drawableHeight
            )
        }

        val gridLineCount = 3
        for (i in 0..gridLineCount) {
            val y = topPadding + drawableHeight * i / gridLineCount
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(plotWidth, y),
                strokeWidth = 1f
            )

            val axisValue = maxValue - range * i / gridLineCount
            val measuredValue = textMeasurer.measure(axisValue.roundToInt().toString(), axisTextStyle)
            drawText(
                textLayoutResult = measuredValue,
                topLeft = Offset(plotWidth + 4f, y - measuredValue.size.height / 2f)
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
            lineTo(points.last().x, topPadding + drawableHeight)
            lineTo(points.first().x, topPadding + drawableHeight)
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

        // X축 라벨: 포인트 위치에 맞춰 그리되, 확대 정도에 따라 겹치지 않을 만큼만 솎아서 표시
        val minLabelSpacingPx = 48f
        var lastLabelX = Float.NEGATIVE_INFINITY
        labels.forEachIndexed { index, label ->
            if (index >= points.size) return@forEachIndexed
            val x = points[index].x
            if (x < -minLabelSpacingPx || x > plotWidth + minLabelSpacingPx) return@forEachIndexed
            if (x - lastLabelX < minLabelSpacingPx) return@forEachIndexed

            val measuredLabel = textMeasurer.measure(label, axisTextStyle)
            val textX = (x - measuredLabel.size.width / 2f).coerceIn(0f, (plotWidth - measuredLabel.size.width).coerceAtLeast(0f))
            drawText(
                textLayoutResult = measuredLabel,
                topLeft = Offset(textX, size.height - bottomAxisHeight + 2f)
            )
            lastLabelX = x
        }
    }
}
