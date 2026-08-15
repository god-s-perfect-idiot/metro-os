package com.metro.launcher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Fixed Start background (WP8.1): wallpaper is screen-locked; transparent tiles sample the
 * portion under their current window bounds so scrolling feels like looking through windows.
 */
data class StartBackgroundViewport(
    val bitmap: ImageBitmap,
    val viewportWidthPx: Float,
    val viewportHeightPx: Float,
)

val LocalStartBackgroundViewport = staticCompositionLocalOf<StartBackgroundViewport?> { null }

/**
 * Draws the Start background cropped to this composable's window rect (parallax factor 0).
 */
fun Modifier.drawStartBackgroundWindow(
    viewport: StartBackgroundViewport?,
): Modifier {
    if (viewport == null) return this
    return composed {
        var windowOffset by remember { mutableStateOf(Offset.Zero) }
        val bmp = viewport.bitmap
        Modifier
            .onGloballyPositioned { coords ->
                windowOffset = coords.positionInWindow()
            }
            .drawBehind {
                val cover = max(
                    viewport.viewportWidthPx / bmp.width.toFloat(),
                    viewport.viewportHeightPx / bmp.height.toFloat(),
                )
                val drawnW = bmp.width * cover
                val drawnH = bmp.height * cover
                val imageLeft = (viewport.viewportWidthPx - drawnW) / 2f
                val imageTop = (viewport.viewportHeightPx - drawnH) / 2f
                drawImage(
                    image = bmp,
                    dstOffset = IntOffset(
                        (imageLeft - windowOffset.x).roundToInt(),
                        (imageTop - windowOffset.y).roundToInt(),
                    ),
                    dstSize = IntSize(
                        drawnW.roundToInt().coerceAtLeast(1),
                        drawnH.roundToInt().coerceAtLeast(1),
                    ),
                )
            }
    }
}
