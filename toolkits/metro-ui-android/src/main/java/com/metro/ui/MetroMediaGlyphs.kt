package com.metro.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Xbox Music / WP8.1 media glyphs shared across Music and launcher now-playing tiles.
 * App-bar transport marks also exist on [MetroSystemIconType] (Play/Pause/Next/Previous).
 */
enum class MetroMediaGlyph {
    Shuffle,
    Repeat,
    RepeatOne,
    Queue,
    Previous,
    Play,
    Pause,
    Next,
}

@Composable
fun MetroMediaGlyphIcon(
    glyph: MetroMediaGlyph,
    modifier: Modifier = Modifier,
    glyphSize: Dp = 26.dp,
    color: Color = MetroTheme.colors.primaryText,
) {
    Canvas(modifier = modifier.size(glyphSize)) { drawMetroMediaGlyph(glyph, color) }
}

@Composable
fun MetroMediaGlyphButton(
    glyph: MetroMediaGlyph,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    color: Color = MetroTheme.colors.primaryText,
    touchTarget: Dp = 48.dp,
    glyphSize: Dp = 26.dp,
) {
    Box(
        modifier = modifier
            .size(touchTarget)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        MetroMediaGlyphIcon(glyph = glyph, glyphSize = glyphSize, color = color)
    }
}

/** Default Xbox Music now-playing transport circle diameter (prev / play-pause / next). */
val MetroMediaTransportButtonSize = 56.dp

private val MetroMediaTransportPressNudge = 4.dp
private const val MetroMediaTransportPressInMs = 70
private const val MetroMediaTransportPressOutMs = 150

/**
 * Circular-ring transport control used on Music now playing and launcher live tiles.
 * Prefer the [MetroSystemIconType] overload for play / pause / previous / next so live
 * tiles and app-bar chrome share the same toolkit SVG glyphs.
 */
@Composable
fun MetroMediaTransportButton(
    glyph: MetroMediaGlyph,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    buttonSize: Dp = MetroMediaTransportButtonSize,
    color: Color = MetroTheme.colors.primaryText,
    enabled: Boolean = true,
) {
    MetroMediaTransportButtonFrame(
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        buttonSize = buttonSize,
        color = color,
        enabled = enabled,
    ) { displayColor ->
        drawMetroMediaGlyph(glyph, displayColor)
    }
}

/**
 * Circular-ring transport using toolkit chrome glyphs ([MetroSystemIconType.Play] /
 * [MetroSystemIconType.Pause] / [MetroSystemIconType.Previous] / [MetroSystemIconType.Next]).
 */
@Composable
fun MetroMediaTransportButton(
    type: MetroSystemIconType,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    buttonSize: Dp = MetroMediaTransportButtonSize,
    color: Color = MetroTheme.colors.primaryText,
    enabled: Boolean = true,
) {
    require(
        type == MetroSystemIconType.Play ||
            type == MetroSystemIconType.Pause ||
            type == MetroSystemIconType.Previous ||
            type == MetroSystemIconType.Next,
    ) { "MetroMediaTransportButton only accepts Play/Pause/Previous/Next" }
    MetroMediaTransportButtonFrame(
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        buttonSize = buttonSize,
        color = color,
        enabled = enabled,
    ) { displayColor ->
        drawMetroSystemIconGlyph(type, displayColor)
    }
}

@Composable
private fun MetroMediaTransportButtonFrame(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier,
    buttonSize: Dp,
    color: Color,
    enabled: Boolean,
    drawGlyph: DrawScope.(Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val press = remember { Animatable(0f) }
    LaunchedEffect(interactionSource) {
        var cycle: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    cycle?.cancel()
                    cycle = launch {
                        press.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = MetroMediaTransportPressInMs,
                                easing = MetroTransitions.PageEasing,
                            ),
                        )
                    }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    val inbound = cycle
                    cycle = launch {
                        inbound?.join()
                        if (press.value > 0f) {
                            press.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(
                                    durationMillis = MetroMediaTransportPressOutMs,
                                    easing = MetroTransitions.PageEasing,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
    val nudgePx = with(LocalDensity.current) { MetroMediaTransportPressNudge.toPx() }
    val pressAmount = press.value
    val displayColor = if (enabled) color else color.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .graphicsLayer(
                translationX = -nudgePx * pressAmount,
                translationY = nudgePx * pressAmount,
            )
            .size(buttonSize)
            .clip(CircleShape)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(buttonSize)) {
            val d = size.minDimension
            val ring = d * 0.035f
            drawCircle(color = displayColor, radius = d / 2f - ring, style = Stroke(width = ring))
            drawGlyph(displayColor)
        }
    }
}

fun DrawScope.drawMetroMediaGlyph(glyph: MetroMediaGlyph, color: Color) {
    when (glyph) {
        MetroMediaGlyph.Shuffle -> drawShuffleGlyph(color)
        MetroMediaGlyph.Repeat -> drawRepeatGlyph(color)
        MetroMediaGlyph.RepeatOne -> {
            drawRepeatGlyph(color)
            drawRepeatOneNumeral(color)
        }
        MetroMediaGlyph.Queue -> drawQueueGlyph(color)
        MetroMediaGlyph.Previous -> drawPreviousGlyph(color)
        MetroMediaGlyph.Play -> drawPlayGlyph(color)
        MetroMediaGlyph.Pause -> drawPauseGlyph(color)
        MetroMediaGlyph.Next -> drawNextGlyph(color)
    }
}

private fun DrawScope.drawRepeatOneNumeral(color: Color) {
    val s = size.minDimension
    val ox = (size.width - s) / 2f
    val oy = (size.height - s) / 2f
    val strokeWidth = s * MetroSystemIconStrokeFraction * 0.88f
    drawLine(
        color,
        Offset(ox + 0.5f * s, oy + 0.35f * s),
        Offset(ox + 0.5f * s, oy + 0.66f * s),
        strokeWidth,
        StrokeCap.Butt,
    )
    drawLine(
        color,
        Offset(ox + 0.39f * s, oy + 0.45f * s),
        Offset(ox + 0.5f * s, oy + 0.35f * s),
        strokeWidth,
        StrokeCap.Butt,
    )
}
