package com.reborn.feature.admin.data.component.section

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 4f

@Composable
fun DataLineChartSection(
    labels: List<String>,
    values: List<Float>,
    hasData: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!hasData) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(RebornTheme.color.grayScale100),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "아직 데이터가 수집되지 않았습니다.",
                style = RebornTheme.typography.bodyMedium,
                color = RebornTheme.color.grayScale500
            )
        }
        return
    }

    val lineColor = RebornTheme.color.grayScale800
    val gridColor = RebornTheme.color.grayScale300
    val axisLabelMaskColor = Color.White
    val axisTextStyle = RebornTheme.typography.caption.copy(color = RebornTheme.color.grayScale500)

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textHorizontalPaddingPx = with(density) { 12.dp.toPx() }
    val topPaddingPx = with(density) { 8.dp.toPx() }
    val bottomAxisHeightPx = with(density) { 24.dp.toPx() }

    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }

    fun clampOffset(currentScale: Float, currentOffset: Float): Float {
        val contentWidth = canvasWidthPx * currentScale
        val minOffset = (canvasWidthPx - contentWidth).coerceAtMost(0f)
        return currentOffset.coerceIn(minOffset, 0f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(vertical = 12.dp)
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

        // 그래프(선/그리드)는 끝에서 끝까지 꽉 채우고, 글자(축 라벨)만 좌우 12dp 안쪽으로 들어가서 표시
        val topPadding = topPaddingPx
        val bottomAxisHeight = bottomAxisHeightPx
        val plotWidth = size.width
        val drawableHeight = size.height - topPadding - bottomAxisHeight

        val contentWidth = plotWidth * scale
        val stepX = contentWidth / (values.size - 1)
        val pointsX = values.indices.map { index -> offsetX + stepX * index }

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

        // 아래(X축) 라벨을 먼저 그리고, 오른쪽(Y축) 라벨을 나중에 그려서 가로 스크롤 시 겹치더라도 오른쪽 라벨이 위에 보이도록 함
        val maxLabelCount = 6
        val visibleIndices = points.indices.filter { index -> points[index].x in 0f..plotWidth }
        if (visibleIndices.isNotEmpty()) {
            val stride = ceil(visibleIndices.size / maxLabelCount.toFloat()).toInt().coerceAtLeast(1)
            visibleIndices.filterIndexed { i, _ -> i % stride == 0 }.forEach { index ->
                val label = labels.getOrNull(index) ?: return@forEach
                val x = points[index].x
                val measuredLabel = textMeasurer.measure(label, axisTextStyle)
                val maxLabelX = (size.width - textHorizontalPaddingPx - measuredLabel.size.width)
                    .coerceAtLeast(textHorizontalPaddingPx)
                val textX = (x - measuredLabel.size.width / 2f)
                    .coerceIn(textHorizontalPaddingPx, maxLabelX)
                drawText(
                    textLayoutResult = measuredLabel,
                    topLeft = Offset(textX, size.height - bottomAxisHeight)
                )
            }
        }

        val measuredValues = (0..gridLineCount).map { i ->
            val axisValue = maxValue - range * i / gridLineCount
            textMeasurer.measure(axisValue.roundToInt().toString(), axisTextStyle)
        }
        val axisLabelAreaWidth = (measuredValues.maxOfOrNull { it.size.width } ?: 0) + textHorizontalPaddingPx
        drawRect(
            color = axisLabelMaskColor,
            topLeft = Offset(size.width - axisLabelAreaWidth, 0f),
            size = Size(axisLabelAreaWidth, size.height)
        )
        measuredValues.forEachIndexed { i, measuredValue ->
            val y = topPadding + drawableHeight * i / gridLineCount
            val valueX = size.width - textHorizontalPaddingPx - measuredValue.size.width
            drawText(
                textLayoutResult = measuredValue,
                topLeft = Offset(valueX, y - measuredValue.size.height / 2f)
            )
        }
    }
}
