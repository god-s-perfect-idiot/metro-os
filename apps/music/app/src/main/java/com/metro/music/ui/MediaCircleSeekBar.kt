package com.metro.music.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.metro.music.data.LibraryLogic
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

private val SeekRowHeight = 40.dp
private val SeekLabelGap = 12.dp
private val SeekTrackThickness = 2.dp
private val SeekThumbDiameter = 14.dp
private val SeekThumbStroke = 3.dp

/**
 * Xbox Music now-playing scrubber: elapsed time, a hairline track carrying a hollow circular
 * thumb, then time remaining — not the rectangular `MetroSlider` thumb.
 *
 * Geometry measured off `references/images/hub_nowplaying_dark_green.jpg` (768x1280 capture,
 * 1.6x): 2dp track at 20% foreground, white played segment, 14dp ring with a 3dp stroke and an
 * empty centre. The caller sizes the row to the album art width so it sits directly under the art.
 */
@Composable
fun MediaCircleSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val seekable = durationMs > 0L
    val currentOnSeek by rememberUpdatedState(onSeek)
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    val playedFraction = if (seekable) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val fraction = scrubFraction ?: playedFraction
    // While scrubbing the labels follow the thumb, not the player's last reported position.
    val labelPositionMs = scrubFraction?.let { (it * durationMs).toLong() } ?: positionMs

    val foreground = MetroTheme.colors.primaryText
    val trackColor = foreground.copy(alpha = 0.2f)
    val thumbRadiusPx = with(LocalDensity.current) { SeekThumbDiameter.toPx() / 2f }

    fun fractionAt(x: Float, width: Int): Float {
        val travel = (width - thumbRadiusPx * 2f).coerceAtLeast(1f)
        return ((x - thumbRadiusPx) / travel).coerceIn(0f, 1f)
    }

    Row(
        modifier = modifier.height(SeekRowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroText(
            text = LibraryLogic.formatDuration(labelPositionMs),
            style = MetroTextStyle.Body,
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(SeekRowHeight)
                .padding(horizontal = SeekLabelGap)
                // Every change is consumed from the first press, otherwise the hub panorama
                // pager treats a scrub as a page swipe and steals the gesture.
                .pointerInput(seekable, durationMs) {
                    if (!seekable) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        try {
                            scrubFraction = fractionAt(down.position.x, size.width)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                change.consume()
                                if (change.changedToUpIgnoreConsumed()) break
                                scrubFraction = fractionAt(change.position.x, size.width)
                            }
                            scrubFraction?.let { currentOnSeek((it * durationMs).toLong()) }
                        } finally {
                            // Leaving a stale scrub fraction would freeze the thumb if the
                            // gesture is cancelled mid-drag.
                            scrubFraction = null
                        }
                    }
                },
        ) {
            val centerY = size.height / 2f
            val travel = (size.width - thumbRadiusPx * 2f).coerceAtLeast(0f)
            val thumbX = thumbRadiusPx + fraction * travel
            val strokePx = SeekThumbStroke.toPx()
            val holeRadiusPx = thumbRadiusPx - strokePx

            drawLine(
                color = trackColor,
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = SeekTrackThickness.toPx(),
                cap = StrokeCap.Butt,
            )
            // Played segment stops at the ring's opening so the thumb centre stays empty.
            val playedEnd = thumbX - holeRadiusPx
            if (playedEnd > 0f) {
                drawLine(
                    color = foreground,
                    start = Offset(0f, centerY),
                    end = Offset(playedEnd, centerY),
                    strokeWidth = SeekTrackThickness.toPx(),
                    cap = StrokeCap.Butt,
                )
            }
            drawCircle(
                color = foreground,
                radius = thumbRadiusPx - strokePx / 2f,
                center = Offset(thumbX, centerY),
                style = Stroke(width = strokePx),
            )
        }
        MetroText(
            text = LibraryLogic.formatRemaining(labelPositionMs, durationMs),
            style = MetroTextStyle.Body,
        )
    }
}
