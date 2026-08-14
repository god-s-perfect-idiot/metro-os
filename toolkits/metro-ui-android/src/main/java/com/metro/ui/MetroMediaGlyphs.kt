package com.metro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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

fun DrawScope.drawMetroMediaGlyph(glyph: MetroMediaGlyph, color: Color) {
    when (glyph) {
        MetroMediaGlyph.Shuffle -> drawShuffleGlyph(color)
        MetroMediaGlyph.Repeat -> drawRepeatGlyph(color)
        MetroMediaGlyph.RepeatOne -> {
            drawRepeatGlyph(color)
            drawRepeatOneNumeral(color)
        }
        MetroMediaGlyph.Queue -> drawQueueGlyph(color)
        MetroMediaGlyph.Previous -> drawSkipGlyph(color, forward = false)
        MetroMediaGlyph.Play -> drawPlayGlyph(color)
        MetroMediaGlyph.Pause -> drawPauseGlyph(color)
        MetroMediaGlyph.Next -> drawSkipGlyph(color, forward = true)
    }
}

private fun DrawScope.drawShuffleGlyph(color: Color) {
    val s = size.minDimension
    val ox = (size.width - s) / 2f
    val oy = (size.height - s) / 2f
    val stroke = Stroke(width = s * 0.09f, cap = StrokeCap.Round)
    val top = 0.27f
    val bottom = 0.73f

    listOf(top to bottom, bottom to top).forEach { (from, to) ->
        val path = Path().apply {
            moveTo(ox + 0.05f * s, oy + from * s)
            cubicTo(
                ox + 0.32f * s, oy + from * s,
                ox + 0.48f * s, oy + to * s,
                ox + 0.76f * s, oy + to * s,
            )
        }
        drawPath(path, color, style = stroke)
        drawArrowHead(
            color = color,
            tip = Offset(ox + 0.97f * s, oy + to * s),
            direction = Offset(1f, 0f),
            length = s * 0.21f,
            halfWidth = s * 0.14f,
        )
    }
}

private fun DrawScope.drawRepeatGlyph(color: Color) {
    val s = size.minDimension
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = s * 0.34f
    val strokeWidth = s * 0.09f

    drawArc(
        color = color,
        startAngle = 290f,
        sweepAngle = 305f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
    )

    val headAngle = 235f * (PI / 180f).toFloat()
    val end = Offset(
        center.x + radius * cos(headAngle),
        center.y + radius * sin(headAngle),
    )
    val tangent = Offset(cos(headAngle + PI.toFloat() / 2f), sin(headAngle + PI.toFloat() / 2f))
    val length = s * 0.26f
    drawArrowHead(
        color = color,
        tip = Offset(end.x + tangent.x * length, end.y + tangent.y * length),
        direction = tangent,
        length = length,
        halfWidth = s * 0.16f,
    )
}

private fun DrawScope.drawRepeatOneNumeral(color: Color) {
    val s = size.minDimension
    val ox = (size.width - s) / 2f
    val oy = (size.height - s) / 2f
    val strokeWidth = s * 0.075f
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

private fun DrawScope.drawQueueGlyph(color: Color) {
    val s = size.minDimension
    val ox = (size.width - s) / 2f
    val oy = (size.height - s) / 2f
    val strokeWidth = s * 0.105f
    val dotRadius = strokeWidth * 0.55f
    listOf(0.24f, 0.5f, 0.76f).forEach { row ->
        val y = oy + row * s
        drawCircle(color, dotRadius, Offset(ox + dotRadius, y))
        drawLine(
            color,
            Offset(ox + 0.22f * s, y),
            Offset(ox + s - strokeWidth / 2f, y),
            strokeWidth,
            StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawArrowHead(
    color: Color,
    tip: Offset,
    direction: Offset,
    length: Float,
    halfWidth: Float,
) {
    val base = Offset(tip.x - direction.x * length, tip.y - direction.y * length)
    val normal = Offset(-direction.y, direction.x)
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(base.x + normal.x * halfWidth, base.y + normal.y * halfWidth)
        lineTo(base.x - normal.x * halfWidth, base.y - normal.y * halfWidth)
        close()
    }
    drawPath(path, color)
}
