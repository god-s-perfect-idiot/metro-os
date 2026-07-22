package com.metro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * WP8.1 slider — accent fill, 4dp track, 24dp circular thumb (METRO-UX-LANGUAGE §6.13).
 * [steps] follows Compose Slider semantics: number of discrete intermediate ticks
 * (tick count = steps + 2 including endpoints). Prefer [MetroStepSlider] for N positions.
 */
@Composable
fun MetroSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
) {
    val accent = MetroTheme.colors.accent
    val trackColor = MetroTheme.colors.secondaryText.copy(alpha = 0.45f)
    val thumbSize = 24.dp
    val trackHeight = 4.dp

    fun snap(raw: Float): Float {
        val coerced = raw.coerceIn(valueRange.start, valueRange.endInclusive)
        if (steps <= 0) return coerced
        val tickCount = steps + 2
        val span = valueRange.endInclusive - valueRange.start
        if (span == 0f) return coerced
        val t = (coerced - valueRange.start) / span
        val idx = (t * (tickCount - 1)).roundToInt().coerceIn(0, tickCount - 1)
        return valueRange.start + (idx / (tickCount - 1).toFloat()) * span
    }

    fun fractionOf(v: Float): Float {
        val span = (valueRange.endInclusive - valueRange.start).takeIf { it != 0f } ?: 1f
        return ((v - valueRange.start) / span).coerceIn(0f, 1f)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val thumbPx = with(LocalDensity.current) { thumbSize.toPx() }
        val travel = (widthPx - thumbPx).coerceAtLeast(1f)
        var fraction by remember(value, valueRange, steps) {
            mutableFloatStateOf(fractionOf(snap(value)))
        }
        fraction = fractionOf(snap(value))

        fun setFromX(x: Float) {
            if (!enabled) return
            val f = ((x - thumbPx / 2f) / travel).coerceIn(0f, 1f)
            val next = snap(valueRange.start + f * (valueRange.endInclusive - valueRange.start))
            fraction = fractionOf(next)
            onValueChange(next)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .pointerInput(enabled, valueRange, steps, widthPx) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset -> setFromX(offset.x) }
                }
                .pointerInput(enabled, valueRange, steps, widthPx) {
                    if (!enabled) return@pointerInput
                    detectDragGestures { change, _ ->
                        setFromX(change.position.x)
                        change.consume()
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(trackHeight)
                    .background(trackColor, RectangleShape),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(accent, RectangleShape),
                )
            }
            Box(
                modifier = Modifier
                    .offset { IntOffset((fraction * travel).roundToInt(), 0) }
                    .align(Alignment.CenterStart)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(MetroTheme.colors.primaryText),
            )
        }
    }
}

/** Discrete slider for a fixed number of positions (e.g. 7 text-size steps). */
@Composable
fun MetroStepSlider(
    index: Int,
    onIndexChange: (Int) -> Unit,
    stepCount: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val max = (stepCount - 1).coerceAtLeast(1)
    MetroSlider(
        value = index.coerceIn(0, max).toFloat(),
        onValueChange = { v -> onIndexChange(v.roundToInt().coerceIn(0, max)) },
        modifier = modifier,
        valueRange = 0f..max.toFloat(),
        steps = (stepCount - 2).coerceAtLeast(0),
        enabled = enabled,
    )
}
