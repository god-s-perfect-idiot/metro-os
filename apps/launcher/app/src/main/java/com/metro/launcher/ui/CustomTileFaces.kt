package com.metro.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.hypot
import kotlin.math.min

/** Google Chrome brand colors (Material / product palette). */
internal object ChromeTileColors {
    val Red = Color(0xFFEA4335)
    val Yellow = Color(0xFFFBBC04)
    val Green = Color(0xFF34A853)
    val Blue = Color(0xFF4285F4)
    val Ring = Color.White
}

private val ChromePackages = setOf(
    "com.android.chrome",
    "com.chrome.beta",
    "com.chrome.dev",
    "com.chrome.canary",
    "com.google.android.apps.chrome",
)

/** True when [packageName] may offer the legacy Chrome Start face in tile customize. */
fun isChromeTilePackage(packageName: String): Boolean = packageName in ChromePackages

/**
 * Custom Chrome Start face: square split into three brand wedges with a blue center disc.
 *
 * Wedges use a circumradius so the three colors fill the tile corners (clipped to the square).
 * Optional [title] overlays bottom-left on medium/wide tiles.
 */
@Composable
fun ChromeTileContent(
    title: String? = null,
    modifier: Modifier = Modifier,
) {
    // Circumradius wedges paint past the square edges so corners stay filled — clip here
    // so they never bleed into Start gutters or the tiles-tab cell padding.
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerRadius = hypot(size.width.toDouble(), size.height.toDouble()).toFloat() / 2f
            val minSide = min(size.width, size.height)
            val blueRadius = minSide * 0.22f
            val ringRadius = blueRadius * 1.28f
            val arcTopLeft = Offset(cx - outerRadius, cy - outerRadius)
            val arcSize = Size(outerRadius * 2f, outerRadius * 2f)

            // Canvas angles: 0° = east, clockwise. Red top-right, green bottom, yellow left.
            drawArc(
                color = ChromeTileColors.Red,
                startAngle = 270f,
                sweepAngle = 120f,
                useCenter = true,
                topLeft = arcTopLeft,
                size = arcSize,
            )
            drawArc(
                color = ChromeTileColors.Green,
                startAngle = 30f,
                sweepAngle = 120f,
                useCenter = true,
                topLeft = arcTopLeft,
                size = arcSize,
            )
            drawArc(
                color = ChromeTileColors.Yellow,
                startAngle = 150f,
                sweepAngle = 120f,
                useCenter = true,
                topLeft = arcTopLeft,
                size = arcSize,
            )
            drawCircle(
                color = ChromeTileColors.Ring,
                radius = ringRadius,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = ChromeTileColors.Blue,
                radius = blueRadius,
                center = Offset(cx, cy),
            )
        }
        if (!title.isNullOrBlank()) {
            val chrome = LocalTileChrome.current
            TileText(
                text = title,
                style = chrome.titleStyle,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        horizontal = chrome.titlePaddingH,
                        vertical = chrome.titlePaddingV,
                    ),
            )
        }
    }
}
