package com.metro.launcher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.launcher.data.TileProgressInfo
import kotlinx.coroutines.delay

/** WP8.1 Store-install overlay: thin rectangular fill flush with the tile bottom. */
internal val TILE_PROGRESS_BAR_HEIGHT = 4.dp

/**
 * Progress overlay that sits on top of the normal Start face (and stays put during flip).
 * Remaining-time copy is drawn just above the bar on medium/wide tiles; 1×1 is bar-only.
 */
@Composable
internal fun BoxScope.TileProgressOverlay(
    progress: TileProgressInfo,
    contentColor: Color,
    showCaption: Boolean,
) {
    val nowMs = rememberProgressClock(progress.countdownEndsAtMs)
    val caption = progress.caption(nowMs)?.takeIf { it.isNotBlank() }
    if (showCaption && !caption.isNullOrBlank()) {
        TileText(
            text = caption,
            style = TileTextStyles.Body,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp)
                .padding(bottom = if (progress.hasBar) TILE_PROGRESS_BAR_HEIGHT + 6.dp else 8.dp),
        )
    }
    if (progress.hasBar) {
        TileProgressBar(
            fraction = progress.fraction,
            indeterminate = progress.indeterminate || progress.fraction == null,
            contentColor = contentColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

@Composable
internal fun TileProgressBar(
    fraction: Float?,
    indeterminate: Boolean,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val track = contentColor.copy(alpha = 0.28f)
    val sweep = if (indeterminate) {
        val transition = rememberInfiniteTransition(label = "tile-progress")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1_400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "tile-progress-sweep",
        ).value
    } else {
        null
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(TILE_PROGRESS_BAR_HEIGHT)
            .clipToBounds(),
    ) {
        drawRect(track)
        if (sweep != null) {
            val barWidth = size.width * 0.35f
            val x = (size.width + barWidth) * sweep - barWidth
            drawRect(
                color = contentColor,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
            )
        } else {
            val width = size.width * (fraction ?: 0f)
            if (width > 0f) {
                drawRect(
                    color = contentColor,
                    size = Size(width, size.height),
                )
            }
        }
    }
}

@Composable
private fun rememberProgressClock(countdownEndsAtMs: Long?): Long {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(countdownEndsAtMs) {
        if (countdownEndsAtMs == null) return@LaunchedEffect
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    return nowMs
}
