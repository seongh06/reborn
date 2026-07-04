package com.reborn.feature.admin.adjust.component.remote

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import kotlin.math.roundToInt

@Composable
fun RulerPickerSection(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 16f..30f,
    step: Float = 0.5f,
    valueDisplayFormatter: (Float) -> String = { valFloat: Float ->
        val isInteger = ((valFloat * 10f).roundToInt() % 10) == 0
        if (isInteger) "${valFloat.toInt()}.0" else "$valFloat"
    }
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RebornTheme.color.grayScale100)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = RebornTheme.typography.titleMedium,
            color = RebornTheme.color.grayScale900
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = valueDisplayFormatter(value),
                style = RebornTheme.typography.bodyLarge,
                color = RebornTheme.color.grayScale900
            )
            RulerWheelSlider(
                value = value,
                onValueChange = onValueChange,
                range = range,
                step = step,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )
        }
    }
}

@Composable
fun RulerWheelSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    lineSpacing: Float = 25f,
    lineColor: Color = Color(0xFFE0E0E0),
    centerLineColor: Color = Color(0xFF757575)
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val totalSteps = ((range.endInclusive - range.start) / step).roundToInt() - 1

    Canvas(
        modifier = modifier
            .semantics {
                contentDescription?.let { this.contentDescription = it }
                progressBarRangeInfo = ProgressBarRangeInfo(value, range, totalSteps.coerceAtLeast(0))
                setProgress { targetValue ->
                    val snappedValue = (targetValue.coerceIn(range) / step).roundToInt() * step
                    if (snappedValue != value) {
                        onValueChange(snappedValue)
                        true
                    } else {
                        false
                    }
                }
            }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    accumulatedDrag -= delta
                    val stepsMoved = (accumulatedDrag / lineSpacing).toInt()

                    if (stepsMoved != 0) {
                        val newValue = value + (stepsMoved * step)
                        val coercedValue = newValue.coerceIn(range)
                        val snappedValue = (coercedValue / step).roundToInt() * step

                        if (snappedValue != value) {
                            onValueChange(snappedValue)
                        }
                        accumulatedDrag %= lineSpacing
                    }
                },
                onDragStopped = {
                    accumulatedDrag = 0f
                }
            )
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f

        val totalStepsInRange = ((range.endInclusive - range.start) / step).toInt()
        val currentValueStep = ((value - range.start) / step).toInt()

        val maxVisibleLines = (centerX / lineSpacing).toInt() + 2

        for (i in -maxVisibleLines..maxVisibleLines) {
            val targetStep = currentValueStep + i
            if (targetStep in 0..totalStepsInRange) {
                val lineX = centerX + (i * lineSpacing) - accumulatedDrag

                if (lineX in 0f..width) {
                    val isCenter = targetStep == currentValueStep
                    val strokeWidth = if (isCenter) 3f else 2f
                    val lineLength = if (isCenter) height * 0.9f else height * 0.5f
                    val currentLineColor = if (isCenter) centerLineColor else lineColor

                    val startY = height - lineLength

                    drawLine(
                        color = currentLineColor,
                        start = Offset(lineX, startY),
                        end = Offset(lineX, height),
                        strokeWidth = strokeWidth
                    )
                }
            }
        }
    }
}