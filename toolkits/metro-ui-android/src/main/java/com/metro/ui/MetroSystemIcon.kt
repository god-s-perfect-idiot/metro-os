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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * WP7/8.1 system icon glyphs inside a thin circular border.
 * Reference: docs/icon-references/
 */
enum class MetroSystemIconType {
    Forward,
    Back,
    Search,
    Close,
    Unpin,
    Resize,
    Add,
    More,
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
    val arm = size.minDimension * 0.12f
    val cx = size.width / 2f
    val cy = size.height / 2f
    drawCircle(color, arm * 0.42f, Offset(cx + arm * 0.1f, cy - arm * 0.55f), style = stroke)
    drawLine(
        color,
        Offset(cx + arm * 0.1f, cy - arm * 0.15f),
        Offset(cx - arm * 0.55f, cy + arm * 0.55f),
        stroke.width,
        StrokeCap.Round,
    )
    drawLine(
        color,
        Offset(cx - arm * 0.75f, cy - arm * 0.75f),
        Offset(cx + arm * 0.75f, cy + arm * 0.75f),
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
