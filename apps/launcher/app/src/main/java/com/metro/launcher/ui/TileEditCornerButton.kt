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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.metro.launcher.data.PinnedTileSize

internal val TileCornerButtonSize = 40.dp
private val TileCornerBorderWidth = 2.dp
private const val GlyphStrokeFraction = 0.095f
private const val UnpinStrokeFraction = 0.075f
private const val GlyphCanvasFraction = 0.54f

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

private fun DrawScope.drawUnpinGlyph(color: Color, stroke: Stroke) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val s = size.minDimension * 0.18f
    // Pushpin head (upper-right of glyph) and shaft angling down-left — WP8.1 unpin silhouette.
    val headCenter = Offset(cx + s * 0.55f, cy - s * 0.95f)
    val headRadius = s * 0.42f
    drawCircle(color, headRadius, headCenter, style = stroke)
    val shaftTop = Offset(headCenter.x - s * 0.08f, headCenter.y + headRadius * 0.55f)
    val shaftBottom = Offset(cx - s * 1.05f, cy + s * 1.05f)
    drawLine(color, shaftTop, shaftBottom, stroke.width, StrokeCap.Round)
    // Strike-through offset from the pin so both shapes stay legible at small size.
    drawLine(
        color,
        Offset(cx - s * 1.15f, cy - s * 0.35f),
        Offset(cx + s * 1.15f, cy + s * 1.35f),
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
    val arm = size.minDimension * 0.20f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val (start, end) = if (downRight) {
        Offset(cx - arm * 0.8f, cy - arm * 0.8f) to Offset(cx + arm * 0.8f, cy + arm * 0.8f)
    } else {
        Offset(cx + arm * 0.8f, cy + arm * 0.8f) to Offset(cx - arm * 0.8f, cy - arm * 0.8f)
    }
    drawLine(color, start, end, stroke.width, StrokeCap.Butt)
    val head = arm * 0.36f
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
    val arm = size.minDimension * 0.20f
    val cx = size.width / 2f
    val cy = size.height / 2f
    drawLine(color, Offset(cx - arm * 0.9f, cy), Offset(cx + arm * 0.55f, cy), stroke.width, StrokeCap.Butt)
    val path = Path().apply {
        moveTo(cx + arm * 0.1f, cy - arm * 0.65f)
        lineTo(cx + arm * 0.9f, cy)
        lineTo(cx + arm * 0.1f, cy + arm * 0.65f)
    }
    drawPath(path, color, style = stroke)
}
