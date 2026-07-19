package com.metro.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val LoaderCycleMs = 1800
private val DotSize = 3.dp
private val TrackWidth = 140.dp
private val DotStartOffsets = listOf((-13).dp, (-26).dp, (-39).dp, (-52).dp)
private val DotDelaysMs = listOf(150, 300, 450, 600)
private val DotEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
private val DotEaseInOut = CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)

/**
 * WP8.1 indeterminate “dancing dots” progress indicator — accent 3dp squares that
 * sweep across a clipped track. Port of the classic ellipsis-loader animation.
 *
 * Prefer [MetroLoadingScreen] for full-page awaits; use this alone for inline progress.
 */
@Composable
fun MetroLoadingDots(
    modifier: Modifier = Modifier,
    color: Color = MetroTheme.colors.accent,
) {
    val density = LocalDensity.current
    val containerShiftEndPx = with(density) { 20.dp.toPx() }
    val travelMidPx = with(density) { 98.dp.toPx() }
    val travelEndPx = with(density) { 212.dp.toPx() }
    val infinite = rememberInfiniteTransition(label = "MetroLoadingDots")

    val containerProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = LoaderCycleMs
                0f at 0
                0f at (LoaderCycleMs * 0.25f).toInt()
                1f at LoaderCycleMs
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "containerShift",
    )

    Box(
        modifier = modifier
            .width(TrackWidth)
            .height(DotSize)
            .graphicsLayer { translationX = containerProgress * containerShiftEndPx }
            .clipToBounds()
            .semantics {
                contentDescription = "Loading"
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        DotStartOffsets.forEachIndexed { index, startOffset ->
            LoadingDot(
                color = color,
                startOffset = startOffset,
                delayMs = DotDelaysMs[index],
                travelMidPx = travelMidPx,
                travelEndPx = travelEndPx,
                index = index,
            )
        }
    }
}

@Composable
private fun LoadingDot(
    color: Color,
    startOffset: Dp,
    delayMs: Int,
    travelMidPx: Float,
    travelEndPx: Float,
    index: Int,
) {
    val density = LocalDensity.current
    val startPx = with(density) { startOffset.toPx() }
    val infinite = rememberInfiniteTransition(label = "MetroLoadingDot$index")
    val startOffsetSpec = StartOffset(
        offsetMillis = delayMs,
        offsetType = StartOffsetType.Delay,
    )

    val travelPx by infinite.animateFloat(
        initialValue = 0f,
        targetValue = travelEndPx,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = LoaderCycleMs
                0f at 0 using DotEaseOut
                travelMidPx at (LoaderCycleMs * 0.55f).toInt() using DotEaseInOut
                travelEndPx at (LoaderCycleMs * 0.75f).toInt()
                travelEndPx at LoaderCycleMs
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = startOffsetSpec,
        ),
        label = "dotTravel$index",
    )
    val opacity by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = LoaderCycleMs
                1f at 0
                1f at (LoaderCycleMs * 0.55f).toInt()
                0.2f at (LoaderCycleMs * 0.90f).toInt()
                1f at LoaderCycleMs
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = startOffsetSpec,
        ),
        label = "dotOpacity$index",
    )

    Box(
        modifier = Modifier
            .size(DotSize)
            .graphicsLayer {
                translationX = startPx + travelPx
                alpha = opacity
            }
            .background(color),
    )
}

/**
 * Full-page loading surface for awaited work — centered label with WP8.1 dancing
 * dots below. Theme background fills the page; call from any screen while blocking.
 *
 * ```kotlin
 * if (isLoading) {
 *     MetroLoadingScreen()
 * } else {
 *     Content()
 * }
 * ```
 */
@Composable
fun MetroLoadingScreen(
    modifier: Modifier = Modifier,
    message: String = "Loading...",
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .semantics {
                contentDescription = message
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BasicText(
                text = message,
                style = TextStyle(
                    fontFamily = MetroFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    color = MetroTheme.colors.primaryText,
                ),
            )
            MetroLoadingDots()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 640)
@Composable
private fun MetroLoadingScreenDarkPreview() {
    MetroTheme(darkTheme = true) {
        MetroLoadingScreen()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 640)
@Composable
private fun MetroLoadingScreenLightPreview() {
    MetroTheme(darkTheme = false) {
        MetroLoadingScreen()
    }
}
