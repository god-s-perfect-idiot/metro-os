package com.metro.music.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.metro.ui.MetroTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Xbox Music now-playing glyphs, drawn to match the WP8.1 capture in
 * `references/images/nowplaying_dark_green.jpg`:
 * crossed shuffle arrows, a clockwise repeat ring, a bulleted queue list, and
 * transport marks (bar + double triangle) inside a thin circular outline.
 */
enum class MediaGlyph {
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
fun MediaGlyphIcon(
    glyph: MediaGlyph,
    modifier: Modifier = Modifier,
    glyphSize: Dp = 26.dp,
    color: Color = MetroTheme.colors.primaryText,
) {
    Canvas(modifier = modifier.size(glyphSize)) { drawMediaGlyph(glyph, color) }
}

/** Side glyph next to the album art (shuffle / repeat / queue). */
@Composable
fun MediaGlyphButton(
    glyph: MediaGlyph,
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
        MediaGlyphIcon(glyph = glyph, glyphSize = glyphSize, color = color)
    }
}

/**
 * Diameter of the now-playing transport circles. The WP8.1 capture spaces them one diameter
 * apart, so callers reuse this for the row gap.
 */
val MediaTransportButtonSize = 56.dp

/** Prev / play-pause / next: glyph inside a thin, unfilled circle — all three the same size. */
@Composable
fun MediaTransportButton(
    glyph: MediaGlyph,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    buttonSize: Dp = MediaTransportButtonSize,
    color: Color = MetroTheme.colors.primaryText,
) {
    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(CircleShape)
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
        Canvas(modifier = Modifier.size(buttonSize)) {
            val d = size.minDimension
            val ring = d * 0.035f
            drawCircle(color = color, radius = d / 2f - ring, style = Stroke(width = ring))
            drawMediaGlyph(glyph, color)
        }
    }
}

private fun DrawScope.drawMediaGlyph(glyph: MediaGlyph, color: Color) {
    when (glyph) {
        MediaGlyph.Shuffle -> drawShuffleGlyph(color)
        MediaGlyph.Repeat -> drawRepeatGlyph(color)
        MediaGlyph.RepeatOne -> {
            drawRepeatGlyph(color)
            drawRepeatOneNumeral(color)
        }
        MediaGlyph.Queue -> drawQueueGlyph(color)
        MediaGlyph.Previous -> drawSkipGlyph(color, forward = false)
        MediaGlyph.Play -> drawPlayGlyph(color)
        MediaGlyph.Pause -> drawPauseGlyph(color)
        MediaGlyph.Next -> drawSkipGlyph(color, forward = true)
    }
}

/** Two curves that cross, each ending in a right-pointing head. */
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

/** Near-full clockwise ring with the head at the left edge of the top gap. */
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

/** Three bulleted rows — WP8.1 "playlist" mark. */
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

/** Double triangle with the end-stop bar on the travel side. */
private fun DrawScope.drawSkipGlyph(color: Color, forward: Boolean) {
    val d = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    val height = d * 0.34f
    val barWidth = d * 0.07f
    val triangleWidth = d * 0.19f
    val gap = d * 0.02f
    val totalWidth = barWidth + triangleWidth * 2f + gap * 2f
    var x = cx - totalWidth / 2f

    fun triangle(left: Float) {
        val apexX = if (forward) left + triangleWidth else left
        val baseX = if (forward) left else left + triangleWidth
        val path = Path().apply {
            moveTo(apexX, cy)
            lineTo(baseX, cy - height / 2f)
            lineTo(baseX, cy + height / 2f)
            close()
        }
        drawPath(path, color)
    }

    if (forward) {
        triangle(x); x += triangleWidth + gap
        triangle(x); x += triangleWidth + gap
        drawRect(color, Offset(x, cy - height / 2f), Size(barWidth, height))
    } else {
        drawRect(color, Offset(x, cy - height / 2f), Size(barWidth, height))
        x += barWidth + gap
        triangle(x); x += triangleWidth + gap
        triangle(x)
    }
}

private fun DrawScope.drawPlayGlyph(color: Color) {
    val d = size.minDimension
    val height = d * 0.34f
    val width = d * 0.28f
    // Nudged right so the triangle looks optically centred in the circle.
    val left = size.width / 2f - width / 2f + d * 0.03f
    val cy = size.height / 2f
    val path = Path().apply {
        moveTo(left, cy - height / 2f)
        lineTo(left + width, cy)
        lineTo(left, cy + height / 2f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawPauseGlyph(color: Color) {
    val d = size.minDimension
    val height = d * 0.34f
    val barWidth = d * 0.085f
    val gap = d * 0.085f
    val cy = size.height / 2f
    val left = size.width / 2f - (barWidth * 2f + gap) / 2f
    drawRect(color, Offset(left, cy - height / 2f), Size(barWidth, height))
    drawRect(color, Offset(left + barWidth + gap, cy - height / 2f), Size(barWidth, height))
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
