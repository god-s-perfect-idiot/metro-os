package com.metro.widgets.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.ui.MetroColors
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import com.metro.widgets.data.NotifierPeekLines
import com.metro.widgets.data.NotifierTraySnapshot
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

private const val TILE_FLIP_HOLD_MS = 5_000L
private const val TILE_FLIP_STAGGER_MAX_MS = 4_000L
private const val TILE_FLIP_HOLD_JITTER_MS = 1_200L
private const val TILE_FLIP_CAMERA_DISTANCE = 16f

private val TileFlipHalfAnimation = MetroTransitions.tileFlipHalfTween<Float>()
private val TileFlipSettleAnimation = MetroTransitions.tileFlipSettleSpring<Float>()

/**
 * Notifier catalog tile — flips only between tray notification peeks (no static
 * “notifier” front face). Idle when access is denied or the tray is empty.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotifierTileFace(
    snapshot: NotifierTraySnapshot,
    accessGranted: Boolean,
    onRequestAccess: () -> Unit,
    onPinToStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = MetroTheme.colors.accent
    val content = MetroColors.tileContentColor(background)
    val peeks = snapshot.peeks

    Box(
        modifier = modifier
            .clipToBounds()
            .combinedClickable(
                onClick = {
                    if (!accessGranted) onRequestAccess()
                },
                onLongClick = onPinToStart,
            ),
    ) {
        when {
            !accessGranted || peeks.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(background),
                ) {
                    NotifierIdleFace(
                        contentColor = content,
                        accessGranted = accessGranted,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                    )
                }
            }
            else -> {
                var peekIndex by remember { mutableIntStateOf(0) }
                val safeIndex = peekIndex.mod(peeks.size.coerceAtLeast(1))
                val peek = peeks[safeIndex]
                // Accent lives only on the rotating face — a stationary fill behind the
                // flip reads as a second tile during the turnstile (Start leaves the slot black).
                NotifierPeekCycleFlip(
                    faceColor = background,
                    peekCount = peeks.size,
                    onAdvance = { next -> peekIndex = next % peeks.size },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    NotifierPeekFace(
                        peek = peek,
                        contentColor = content,
                        badgeCount = snapshot.count,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NotifierIdleFace(
    contentColor: Color,
    accessGranted: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        BasicText(
            text = if (accessGranted) "no notifications" else "allow notification access",
            style = TextStyle(
                color = contentColor,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontFamily = MetroTheme.fontFamily,
                fontWeight = FontWeight.Normal,
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

@Composable
private fun NotifierPeekFace(
    peek: NotifierPeekLines,
    contentColor: Color,
    badgeCount: Int,
    modifier: Modifier = Modifier,
) {
    val lines = buildList {
        peek.title?.takeIf { it.isNotBlank() }?.let { add(it) }
        peek.subtitle?.takeIf { it.isNotBlank() }?.let { add(it) }
        peek.body?.takeIf { it.isNotBlank() }?.let { add(it) }
    }.let { all ->
        when {
            all.size <= 3 -> all
            else -> listOf(all.first(), all.last())
        }
    }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds(),
            ) {
                lines.forEachIndexed { index, line ->
                    val isTitle = index == 0
                    BasicText(
                        text = line,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = if (isTitle) 20.sp else 16.sp,
                            lineHeight = if (isTitle) 24.sp else 20.sp,
                            fontFamily = MetroTheme.fontFamily,
                            fontWeight = FontWeight.Normal,
                        ),
                        maxLines = if (isTitle) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                BasicText(
                    text = peek.appLabel,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontFamily = MetroTheme.fontFamily,
                        fontWeight = FontWeight.Normal,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.padding(end = 28.dp))
            }
        }
        if (badgeCount > 0) {
            NotifierBadge(
                count = badgeCount,
                contentColor = contentColor,
            )
        }
    }
}

@Composable
private fun BoxScope.NotifierBadge(
    count: Int,
    contentColor: Color,
) {
    val display = count.coerceAtMost(99).toString()
    BasicText(
        text = display,
        style = TextStyle(
            color = contentColor,
            fontSize = 28.sp,
            lineHeight = 30.sp,
            fontFamily = MetroTheme.fontFamily,
            fontWeight = FontWeight.Bold,
        ),
        maxLines = 1,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 6.dp, bottom = 4.dp),
    )
}

/**
 * Hold on the current peek, then 600ms flip into the next peek. Never returns to a
 * titled front face — notifications only.
 */
@Composable
private fun NotifierPeekCycleFlip(
    faceColor: Color,
    peekCount: Int,
    onAdvance: (nextIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current.density
    val rotation = remember { Animatable(0f) }
    var peekIndex by remember { mutableIntStateOf(0) }
    val onAdvanceState = rememberUpdatedState(onAdvance)
    val peekCountState = rememberUpdatedState(peekCount)
    val flipSeed = remember { Random.nextInt() }

    LaunchedEffect(flipSeed) {
        if (abs(rotation.value) > 0.01f) {
            rotation.snapTo(0f)
        }
        val rng = Random(flipSeed)
        delay(rng.nextLong(0L, TILE_FLIP_STAGGER_MAX_MS + 1))
        while (true) {
            val count = peekCountState.value.coerceAtLeast(1)
            val jitter = rng.nextLong(-TILE_FLIP_HOLD_JITTER_MS, TILE_FLIP_HOLD_JITTER_MS + 1)
            delay((TILE_FLIP_HOLD_MS + jitter).coerceAtLeast(2_500L))
            if (count <= 1) continue
            rotation.animateTo(90f, animationSpec = TileFlipHalfAnimation)
            peekIndex = (peekIndex + 1) % count
            onAdvanceState.value(peekIndex)
            rotation.snapTo(-90f)
            rotation.animateTo(0f, animationSpec = TileFlipSettleAnimation)
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationX = rotation.value
                transformOrigin = TransformOrigin(0.5f, 0.5f)
                cameraDistance = TILE_FLIP_CAMERA_DISTANCE * density
            }
            .background(faceColor),
        content = content,
    )
}
