package com.metro.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.metro.launcher.data.PinnedTileSize

internal val TileCornerButtonSize = 40.dp
private val TileCornerBorderWidth = 2.dp
private const val GlyphStrokeFraction = 0.095f
private const val UnpinStrokeFraction = 0.090f
private const val GlyphCanvasFraction = 0.58f

/** Glyph shown on the resize corner button for the upcoming tile size transition. */
enum class TileResizeGlyph {
    /** 1×1 → 2×2 */
    DiagonalDownRight,
    /** 2×2 → 4×2 */
    Right,
    /** 4×2 → 1×1 */
    DiagonalUpLeft,
}

fun resizeGlyphForTileSize(size: PinnedTileSize): TileResizeGlyph = when (size) {
    PinnedTileSize.OneByOne -> TileResizeGlyph.DiagonalDownRight
    PinnedTileSize.TwoByTwo -> TileResizeGlyph.Right
    PinnedTileSize.FourByTwo -> TileResizeGlyph.DiagonalUpLeft
}

/**
 * WP8.1 tile edit corner control — solid black disc, white outer ring, sharp glyphs.
 * Centered on the tile corner vertex (half overlaps the tile, half outside).
 */
@Composable
fun TileEditCornerButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    unpin: Boolean = false,
    resizeGlyph: TileResizeGlyph? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(TileCornerButtonSize)
            .background(Color.Black, CircleShape)
            .border(TileCornerBorderWidth, Color.White, CircleShape)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(TileCornerButtonSize * GlyphCanvasFraction)) {
            val strokeWidth = size.minDimension * GlyphStrokeFraction
            val glyphStroke = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Butt,
                join = StrokeJoin.Miter,
            )
            val unpinStroke = Stroke(
                width = size.minDimension * UnpinStrokeFraction,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            when {
                unpin -> drawUnpinGlyph(Color.White, unpinStroke)
                resizeGlyph != null -> drawResizeGlyph(Color.White, glyphStroke, resizeGlyph)
            }
        }
    }
}

/**
 * Classic thumbtack silhouette (flat head, collar, needle) tilted with a slash —
 * the WP8.1 Start tile-edit unpin affordance (Segoe UnPin).
 */
private fun DrawScope.drawUnpinGlyph(color: Color, stroke: Stroke) {
    val min = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    val s = min * 0.42f
    val pivot = Offset(cx, cy)

    rotate(degrees = 38f, pivot = pivot) {
        val pin = Path().apply {
            // Flat push-head
            moveTo(cx - 0.28f * s, cy - 0.95f * s)
            lineTo(cx + 0.28f * s, cy - 0.95f * s)
            lineTo(cx + 0.28f * s, cy - 0.45f * s)
            // Right collar
            lineTo(cx + 0.55f * s, cy - 0.15f * s)
            lineTo(cx + 0.55f * s, cy + 0.05f * s)
            // Taper to needle tip
            lineTo(cx + 0.12f * s, cy + 0.35f * s)
            lineTo(cx, cy + 1.05f * s)
            lineTo(cx - 0.12f * s, cy + 0.35f * s)
            // Left collar
            lineTo(cx - 0.55f * s, cy + 0.05f * s)
            lineTo(cx - 0.55f * s, cy - 0.15f * s)
            lineTo(cx - 0.28f * s, cy - 0.45f * s)
            close()
        }
        drawPath(pin, color)
    }

    // Slash across the pin (top-left → bottom-right); ends extend past the silhouette.
    val arm = s * 1.05f
    drawLine(
        color,
        Offset(cx - arm, cy - arm),
        Offset(cx + arm, cy + arm),
        stroke.width,
        StrokeCap.Round,
    )
}

private fun DrawScope.drawResizeGlyph(color: Color, stroke: Stroke, glyph: TileResizeGlyph) {
    when (glyph) {
        TileResizeGlyph.DiagonalDownRight -> drawDiagonalArrow(color, stroke, downRight = true)
        TileResizeGlyph.Right -> drawRightArrow(color, stroke)
        TileResizeGlyph.DiagonalUpLeft -> drawDiagonalArrow(color, stroke, downRight = false)
    }
}

private fun DrawScope.drawDiagonalArrow(color: Color, stroke: Stroke, downRight: Boolean) {
    val arm = size.minDimension * 0.24f
    val cx = size.width / 2f
    val cy = size.height / 2f
    // Shaft slightly short of the tip so the L-head reads as the terminal, not a stub.
    val (start, end) = if (downRight) {
        Offset(cx - arm * 0.85f, cy - arm * 0.85f) to Offset(cx + arm * 0.85f, cy + arm * 0.85f)
    } else {
        Offset(cx + arm * 0.85f, cy + arm * 0.85f) to Offset(cx - arm * 0.85f, cy - arm * 0.85f)
    }
    drawLine(color, start, end, stroke.width, StrokeCap.Butt)
    // Axis-aligned L tip — WP8.1 resize chevron; sized to match shaft visual weight.
    val head = arm * 0.68f
    if (downRight) {
        val path = Path().apply {
            moveTo(end.x - head, end.y)
            lineTo(end.x, end.y)
            lineTo(end.x, end.y - head)
        }
        drawPath(path, color, style = stroke)
    } else {
        val path = Path().apply {
            moveTo(end.x + head, end.y)
            lineTo(end.x, end.y)
            lineTo(end.x, end.y + head)
        }
        drawPath(path, color, style = stroke)
    }
}

private fun DrawScope.drawRightArrow(color: Color, stroke: Stroke) {
    val arm = size.minDimension * 0.28f
    val cx = size.width / 2f
    val cy = size.height / 2f
    drawLine(color, Offset(cx - arm * 0.95f, cy), Offset(cx + arm * 0.45f, cy), stroke.width, StrokeCap.Butt)
    val path = Path().apply {
        moveTo(cx + arm * 0.05f, cy - arm * 0.72f)
        lineTo(cx + arm * 0.95f, cy)
        lineTo(cx + arm * 0.05f, cy + arm * 0.72f)
    }
    drawPath(path, color, style = stroke)
}
