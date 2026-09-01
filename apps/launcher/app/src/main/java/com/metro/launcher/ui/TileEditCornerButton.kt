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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.metro.launcher.data.PinnedTileSize
import com.metro.ui.MetroSystemIconType
import com.metro.ui.drawMetroSystemIconGlyph

internal val TileCornerButtonSize = 40.dp
private val TileCornerBorderWidth = 2.dp
private const val UnpinGlyphCanvasFraction = 0.58f
/** Diagonal resize arrows — tuned for the corner disc. */
internal const val ResizeGlyphCanvasFraction = 0.8f
/** Forward arrow has more viewBox padding; bump canvas so it matches the diagonals. */
internal const val ResizeForwardGlyphCanvasFraction = 0.94f

/** System icon shown on the resize corner button for the upcoming tile size transition. */
fun resizeIconForTileSize(size: PinnedTileSize): MetroSystemIconType = when (size) {
    PinnedTileSize.OneByOne -> MetroSystemIconType.Resize
    PinnedTileSize.TwoByTwo -> MetroSystemIconType.Forward
    PinnedTileSize.FourByTwo -> MetroSystemIconType.ResizeShrink
}

/** Per-size glyph canvas scale for the resize corner button. */
fun resizeGlyphScaleForTileSize(size: PinnedTileSize): Float = when (size) {
    PinnedTileSize.TwoByTwo -> ResizeForwardGlyphCanvasFraction
    else -> ResizeGlyphCanvasFraction
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
    icon: MetroSystemIconType? = null,
    glyphScale: Float = UnpinGlyphCanvasFraction,
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
        if (icon != null) {
            Canvas(modifier = Modifier.size(TileCornerButtonSize * glyphScale)) {
                drawMetroSystemIconGlyph(icon, Color.White)
            }
        }
    }
}
