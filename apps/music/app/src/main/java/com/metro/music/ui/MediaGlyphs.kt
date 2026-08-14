package com.metro.music.ui

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroMediaGlyph
import com.metro.ui.MetroMediaGlyphButton
import com.metro.ui.MetroMediaGlyphIcon
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import com.metro.ui.drawMetroMediaGlyph
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** App alias for the shared suite media glyph set. */
typealias MediaGlyph = MetroMediaGlyph

@Composable
fun MediaGlyphIcon(
    glyph: MediaGlyph,
    modifier: Modifier = Modifier,
    glyphSize: Dp = 26.dp,
    color: androidx.compose.ui.graphics.Color = MetroTheme.colors.primaryText,
) {
    MetroMediaGlyphIcon(glyph = glyph, modifier = modifier, glyphSize = glyphSize, color = color)
}

@Composable
fun MediaGlyphButton(
    glyph: MediaGlyph,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MetroTheme.colors.primaryText,
    touchTarget: Dp = 48.dp,
    glyphSize: Dp = 26.dp,
) {
    MetroMediaGlyphButton(
        glyph = glyph,
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        color = color,
        touchTarget = touchTarget,
        glyphSize = glyphSize,
    )
}

val MediaTransportButtonSize = 56.dp

internal val MediaTransportPressNudge = 4.dp

private const val MediaTransportPressInMs = 70
private const val MediaTransportPressOutMs = 150

@Composable
fun MediaTransportButton(
    glyph: MediaGlyph,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    buttonSize: Dp = MediaTransportButtonSize,
    color: androidx.compose.ui.graphics.Color = MetroTheme.colors.primaryText,
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
                                durationMillis = MediaTransportPressInMs,
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
                                    durationMillis = MediaTransportPressOutMs,
                                    easing = MetroTransitions.PageEasing,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
    val nudgePx = with(LocalDensity.current) { MediaTransportPressNudge.toPx() }
    val pressAmount = press.value
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
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(buttonSize)) {
            val d = size.minDimension
            val ring = d * 0.035f
            drawCircle(color = color, radius = d / 2f - ring, style = Stroke(width = ring))
            drawMetroMediaGlyph(glyph, color)
        }
    }
}
