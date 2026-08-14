package com.metro.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared WP7/8.1 chrome glyphs for the whole suite.
 *
 * Apps must use these (via [MetroSystemIcon] / [MetroAppBarIcon]) instead of inventing
 * per-app Canvas icons for common actions. App identity art lives in [MetroAppGlyphs].
 *
 * Reference: METRO-UX-LANGUAGE.md §9.
 */
enum class MetroSystemIconType {
    // Navigation / chrome
    Forward,
    Back,
    Search,
    Close,
    Unpin,
    Resize,
    Add,
    More,
    SwitchView,

    // Common app-bar actions
    Phone,
    Message,
    Heart,
    DialPad,
    People,
    Delete,
    Check,

    // Media transport (also see [MetroMediaGlyph] for shuffle/repeat/queue)
    Play,
    Pause,
    Next,
    Previous,
}

@Composable
fun MetroSystemIcon(
    type: MetroSystemIconType,
    modifier: Modifier = Modifier,
    iconSize: Dp = 40.dp,
    color: Color = MetroTheme.colors.primaryText,
    showCircle: Boolean = true,
) {
    Canvas(modifier = modifier.size(iconSize)) {
        val strokeWidth = size.minDimension * 0.05f
        val glyphStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val forwardGlyphStroke = Stroke(
            width = strokeWidth * 1.35f,
            cap = StrokeCap.Butt,
            join = StrokeJoin.Miter,
        )
        if (showCircle) {
            val circleRadius = size.minDimension * 0.42f - strokeWidth
            drawCircle(
                color = color,
                radius = circleRadius,
                style = Stroke(width = strokeWidth),
            )
        }
        when (type) {
            MetroSystemIconType.Forward -> drawForwardGlyph(color, forwardGlyphStroke)
            MetroSystemIconType.Back -> drawBackGlyph(color, glyphStroke)
            MetroSystemIconType.Search -> drawSearchGlyph(color, glyphStroke)
            MetroSystemIconType.Close -> drawCloseGlyph(color, glyphStroke)
            MetroSystemIconType.Unpin -> drawUnpinGlyph(color, glyphStroke)
            MetroSystemIconType.Resize -> drawResizeGlyph(color, glyphStroke)
            MetroSystemIconType.Add -> drawAddGlyph(color, glyphStroke)
            MetroSystemIconType.More -> drawMoreGlyph(color, glyphStroke)
            MetroSystemIconType.SwitchView -> drawSwitchViewGlyph(color, glyphStroke)
            MetroSystemIconType.Phone -> drawViewportPath(phoneHandsetPath, color, 0.72f)
            MetroSystemIconType.Message -> drawViewportPath(messagingBubblePath, color, 0.66f)
            MetroSystemIconType.Heart -> drawHeartGlyph(color)
            MetroSystemIconType.DialPad -> drawDialPadGlyph(color, glyphStroke)
            MetroSystemIconType.People -> drawPeopleGlyph(color, glyphStroke)
            MetroSystemIconType.Delete -> drawDeleteGlyph(color, glyphStroke)
            MetroSystemIconType.Check -> drawCheckGlyph(color, glyphStroke)
            MetroSystemIconType.Play -> drawPlayGlyph(color)
            MetroSystemIconType.Pause -> drawPauseGlyph(color)
            MetroSystemIconType.Next -> drawSkipGlyph(color, forward = true)
            MetroSystemIconType.Previous -> drawSkipGlyph(color, forward = false)
        }
    }
}

/**
 * Tappable system icon with the WP7 circular-outline affordance.
 */
@Composable
fun MetroCircleIconButton(
    type: MetroSystemIconType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    color: Color = MetroTheme.colors.primaryText,
    backgroundColor: Color? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (backgroundColor != null) {
                    Modifier.background(backgroundColor, CircleShape)
                } else {
                    Modifier
                },
            )
            .semantics {
                role = Role.Button
                contentDescription?.let { this.contentDescription = it }
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        MetroSystemIcon(
            type = type,
            iconSize = size * 0.82f,
            color = if (enabled) color else color.copy(alpha = 0.4f),
        )
    }
}

private fun DrawScope.drawForwardGlyph(color: Color, stroke: Stroke) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val arm = size.minDimension * 0.16f
    drawLine(color, Offset(cx - arm, cy), Offset(cx + arm, cy), stroke.width, StrokeCap.Butt)
    val path = Path().apply {
        moveTo(cx + arm * 0.2f, cy - arm * 0.85f)
        lineTo(cx + arm, cy)
        lineTo(cx + arm * 0.2f, cy + arm * 0.85f)
    }
    drawPath(path, color, style = stroke)
}

private fun DrawScope.drawBackGlyph(color: Color, stroke: Stroke) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val arm = size.minDimension * 0.16f
    drawLine(color, Offset(cx - arm, cy), Offset(cx + arm, cy), stroke.width, StrokeCap.Round)
    val path = Path().apply {
        moveTo(cx - arm * 0.2f, cy - arm * 0.85f)
        lineTo(cx - arm, cy)
        lineTo(cx - arm * 0.2f, cy + arm * 0.85f)
    }
    drawPath(path, color, style = stroke)
}

private fun DrawScope.drawSearchGlyph(color: Color, stroke: Stroke) {
    val arm = size.minDimension * 0.16f
    val cx = size.width / 2f + arm * 0.375f
    val cy = size.height / 2f - arm * 0.375f
    drawCircle(color, arm, Offset(cx, cy), style = stroke)
    drawLine(
        color,
        Offset(cx - arm * 0.65f, cy + arm * 0.65f),
        Offset(cx - arm * 1.55f, cy + arm * 1.55f),
        stroke.width,
        StrokeCap.Round,
    )
}

private fun DrawScope.drawCloseGlyph(color: Color, stroke: Stroke) {
    val arm = size.minDimension * 0.14f
    val cx = size.width / 2f
    val cy = size.height / 2f
    drawLine(color, Offset(cx - arm, cy - arm), Offset(cx + arm, cy + arm), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(cx + arm, cy - arm), Offset(cx - arm, cy + arm), stroke.width, StrokeCap.Round)
}

private fun DrawScope.drawUnpinGlyph(color: Color, stroke: Stroke) {
    val min = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    val s = min * 0.28f
    val pivot = Offset(cx, cy)

    rotate(degrees = 38f, pivot = pivot) {
        val pin = Path().apply {
            moveTo(cx - 0.28f * s, cy - 0.95f * s)
            lineTo(cx + 0.28f * s, cy - 0.95f * s)
            lineTo(cx + 0.28f * s, cy - 0.45f * s)
            lineTo(cx + 0.55f * s, cy - 0.15f * s)
            lineTo(cx + 0.55f * s, cy + 0.05f * s)
            lineTo(cx + 0.12f * s, cy + 0.35f * s)
            lineTo(cx, cy + 1.05f * s)
            lineTo(cx - 0.12f * s, cy + 0.35f * s)
            lineTo(cx - 0.55f * s, cy + 0.05f * s)
            lineTo(cx - 0.55f * s, cy - 0.15f * s)
            lineTo(cx - 0.28f * s, cy - 0.45f * s)
            close()
        }
        drawPath(pin, color)
    }

    val arm = s * 1.05f
    drawLine(
        color,
        Offset(cx - arm, cy - arm),
        Offset(cx + arm, cy + arm),
        stroke.width,
        StrokeCap.Round,
    )
}

/** Diagonal arrow toward tile interior (WP8.1 resize affordance). */
private fun DrawScope.drawResizeGlyph(color: Color, stroke: Stroke) {
    val arm = size.minDimension * 0.14f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val start = Offset(cx + arm * 0.55f, cy - arm * 0.75f)
    val end = Offset(cx - arm * 0.75f, cy + arm * 0.55f)
    drawLine(color, start, end, stroke.width, StrokeCap.Round)
    val path = Path().apply {
        moveTo(end.x + arm * 0.38f, end.y)
        lineTo(end.x, end.y)
        lineTo(end.x, end.y - arm * 0.38f)
    }
    drawPath(path, color, style = stroke)
}

private fun DrawScope.drawAddGlyph(color: Color, stroke: Stroke) {
    val arm = size.minDimension * 0.14f
    val cx = size.width / 2f
    val cy = size.height / 2f
    drawLine(color, Offset(cx - arm, cy), Offset(cx + arm, cy), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(cx, cy - arm), Offset(cx, cy + arm), stroke.width, StrokeCap.Round)
}

private fun DrawScope.drawMoreGlyph(color: Color, stroke: Stroke) {
    val r = stroke.width * 1.15f
    val cy = size.height / 2f
    val spacing = size.minDimension * 0.16f
    val cx = size.width / 2f
    drawCircle(color, r, Offset(cx - spacing, cy))
    drawCircle(color, r, Offset(cx, cy))
    drawCircle(color, r, Offset(cx + spacing, cy))
}

/** Two opposing horizontal arrows (calendar "switch view"). */
internal fun DrawScope.drawSwitchViewGlyph(color: Color, stroke: Stroke) {
    val cx = size.width / 2f
    val halfLen = size.minDimension * 0.22f
    val offsetY = size.minDimension * 0.10f
    val head = size.minDimension * 0.085f
    val topY = size.height / 2f - offsetY
    val botY = size.height / 2f + offsetY

    drawLine(color, Offset(cx - halfLen, topY), Offset(cx + halfLen, topY), stroke.width, StrokeCap.Round)
    drawPath(
        Path().apply {
            moveTo(cx + halfLen - head, topY - head)
            lineTo(cx + halfLen, topY)
            lineTo(cx + halfLen - head, topY + head)
        },
        color,
        style = stroke,
    )

    drawLine(color, Offset(cx - halfLen, botY), Offset(cx + halfLen, botY), stroke.width, StrokeCap.Round)
    drawPath(
        Path().apply {
            moveTo(cx - halfLen + head, botY - head)
            lineTo(cx - halfLen, botY)
            lineTo(cx - halfLen + head, botY + head)
        },
        color,
        style = stroke,
    )
}

private const val PHONE_HANDSET_PATH =
    "M34.24,18.2 C33.27,18.41 32.13,19.11 30.54,20.49 C29.71,21.19 28.58,22.17 27.99,22.67 " +
        "C25.33,24.9 22.95,28.47 21.77,32 C20.85,34.81 20.66,36.02 20.66,39.56 C20.65,42.9 " +
        "20.78,43.99 21.51,46.88 C23.49,54.84 29.04,63.8 37.78,73.18 C43.54,79.36 49.88,84.15 " +
        "55.5,86.58 C63.12,89.87 70.19,90 76.54,86.94 C78.52,85.99 80.14,84.87 82.18,83.06 " +
        "C84.81,80.71 85.97,79.49 86.34,78.7 C87.35,76.52 86.51,74.16 83.71,71.35 C82.32,69.94 " +
        "78.75,66.89 77.26,65.83 C74.53,63.89 72.21,63.13 70.08,63.45 C68.18,63.75 66.45,64.93 " +
        "63.02,68.3 C61.91,69.4 61.31,69.87 60.63,70.18 C56.98,71.91 51.58,68.58 44.6,60.29 " +
        "C39.57,54.3 37.35,49.69 38.01,46.57 C38.29,45.24 39.06,44.21 40.66,43.03 C42.67,41.54 " +
        "45.01,39.45 45.68,38.53 C47.18,36.5 47.35,34.09 46.21,31.03 C45.23,28.4 41.56,22.65 " +
        "39.45,20.43 C38.51,19.45 38.03,19.08 37.25,18.69 C36.17,18.15 35.2,18 34.24,18.2 Z"

private val phoneHandsetPath: Path by lazy {
    PathParser().parsePathString(PHONE_HANDSET_PATH).toPath()
}

private const val MESSAGING_BUBBLE_PATH =
    "M18,28c0,-3 2.4,-5.4 5.4,-5.4h56c3,0 5.4,2.4 5.4,5.4v34c0,3 -2.4,5.4 -5.4,5.4H58l14,16l-8,-16H23.4c-3,0 -5.4,-2.4 -5.4,-5.4V28z" +
        "M35.5,40.5a3.2,3.2 0 1,0 6.4,0a3.2,3.2 0 1,0 -6.4,0z" +
        "M35.5,53.5a3.2,3.2 0 1,0 6.4,0a3.2,3.2 0 1,0 -6.4,0z" +
        "M47,45.2h12c1.2,0 2.2,1 2.2,2.2s-1,2.2 -2.2,2.2h-12c-1.2,0 -2.2,-1 -2.2,-2.2s1,-2.2 2.2,-2.2z"

private val messagingBubblePath: Path by lazy {
    PathParser().parsePathString(MESSAGING_BUBBLE_PATH).toPath().apply {
        fillType = PathFillType.EvenOdd
    }
}

internal fun DrawScope.drawViewportPath(path: Path, color: Color, glyphScale: Float) {
    val scale = size.minDimension / 108f * glyphScale
    val cx = size.width / 2f
    val cy = size.height / 2f
    withTransform({
        translate(left = cx, top = cy)
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        translate(left = -54f, top = -54f)
    }) {
        drawPath(path, color)
    }
}

internal fun DrawScope.drawHeartGlyph(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.50f, h * 0.78f)
        cubicTo(w * 0.22f, h * 0.58f, w * 0.10f, h * 0.38f, w * 0.28f, h * 0.26f)
        cubicTo(w * 0.40f, h * 0.18f, w * 0.50f, h * 0.28f, w * 0.50f, h * 0.28f)
        cubicTo(w * 0.50f, h * 0.28f, w * 0.60f, h * 0.18f, w * 0.72f, h * 0.26f)
        cubicTo(w * 0.90f, h * 0.38f, w * 0.78f, h * 0.58f, w * 0.50f, h * 0.78f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawDialPadGlyph(color: Color, stroke: Stroke) {
    val gap = size.width * 0.28f
    val tile = size.width * 0.22f
    for (row in 0..2) {
        for (col in 0..2) {
            drawRect(
                color = color,
                topLeft = Offset(col * gap, row * gap),
                size = Size(tile, tile),
                style = stroke,
            )
        }
    }
}

private fun DrawScope.drawPeopleGlyph(color: Color, stroke: Stroke) {
    val cx = size.width / 2f
    drawCircle(color = color, radius = size.minDimension * 0.14f, style = stroke)
    val path = Path().apply {
        moveTo(cx - size.minDimension * 0.22f, size.height * 0.72f)
        quadraticBezierTo(
            cx,
            size.height * 0.48f,
            cx + size.minDimension * 0.22f,
            size.height * 0.72f,
        )
    }
    drawPath(path, color, style = stroke)
}

private fun DrawScope.drawDeleteGlyph(color: Color, stroke: Stroke) {
    val s = size.minDimension
    val cx = size.width / 2f
    val top = size.height * 0.22f
    val lidY = size.height * 0.32f
    val bottom = size.height * 0.78f
    val left = cx - s * 0.22f
    val right = cx + s * 0.22f
    drawLine(color, Offset(cx - s * 0.12f, top), Offset(cx + s * 0.12f, top), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(left - s * 0.06f, lidY), Offset(right + s * 0.06f, lidY), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(left, lidY), Offset(left + s * 0.04f, bottom), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(right, lidY), Offset(right - s * 0.04f, bottom), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(left + s * 0.04f, bottom), Offset(right - s * 0.04f, bottom), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(cx, lidY + s * 0.06f), Offset(cx, bottom - s * 0.06f), stroke.width, StrokeCap.Round)
}

private fun DrawScope.drawCheckGlyph(color: Color, stroke: Stroke) {
    val s = size.minDimension
    val path = Path().apply {
        moveTo(size.width * 0.22f, size.height * 0.52f)
        lineTo(size.width * 0.42f, size.height * 0.72f)
        lineTo(size.width * 0.78f, size.height * 0.28f)
    }
    drawPath(path, color, style = Stroke(width = stroke.width * 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

internal fun DrawScope.drawPlayGlyph(color: Color) {
    val d = size.minDimension
    val height = d * 0.34f
    val width = d * 0.28f
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

internal fun DrawScope.drawPauseGlyph(color: Color) {
    val d = size.minDimension
    val height = d * 0.34f
    val barWidth = d * 0.085f
    val gap = d * 0.085f
    val cy = size.height / 2f
    val left = size.width / 2f - (barWidth * 2f + gap) / 2f
    drawRect(color, Offset(left, cy - height / 2f), Size(barWidth, height))
    drawRect(color, Offset(left + barWidth + gap, cy - height / 2f), Size(barWidth, height))
}

internal fun DrawScope.drawSkipGlyph(color: Color, forward: Boolean) {
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
