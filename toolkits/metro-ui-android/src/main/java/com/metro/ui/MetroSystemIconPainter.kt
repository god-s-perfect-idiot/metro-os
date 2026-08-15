package com.metro.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

/**
 * Tintable [Painter] for [MetroSystemIconType] glyphs.
 *
 * Draws in opaque black so `Icon(tint=…)` / Snygg foreground ColorFilter replaces the ink
 * (same contract as Material Icons ImageVectors).
 */
fun metroSystemIconPainter(
    type: MetroSystemIconType,
    intrinsicSize: Size = Size(24f, 24f),
): Painter = MetroSystemIconPainter(type, intrinsicSize)

@Composable
fun rememberMetroSystemIconPainter(type: MetroSystemIconType): Painter {
    return remember(type) { metroSystemIconPainter(type) }
}

private class MetroSystemIconPainter(
    private val type: MetroSystemIconType,
    private val size: Size,
) : Painter() {
    override val intrinsicSize: Size get() = size

    override fun DrawScope.onDraw() {
        // ColorFilter.SrcIn from Icon/SnyggIcon replaces non-transparent pixels with tint.
        drawMetroSystemIconGlyph(type, Color.Black)
    }
}

/** Default glyph size used on SIP smartbar action tiles / toggles. */
val MetroSystemIconDefaultSize = 32.dp
