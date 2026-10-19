package com.metro.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Fraction of measured brand width used as parallax travel when the title fits on-screen.
 * Longer titles (e.g. `metro music`) drift farther than short ones (`hub`); overflowing
 * brands still travel at least their clipped width so the tail is revealed.
 */
const val MetroPanoramaBrandParallaxWidthFraction = 0.38f

/**
 * Default trailing bleed after long panoramic brands (Music) so the title overflows the
 * viewport and parallax has room to scroll past the last glyph.
 */
val MetroPanoramaBrandTrailingSlack = 128.dp

/**
 * Giant panoramic hub brand (e.g. `metro music`, `hub`, `news`) — ExtraLight 96sp, flush-left,
 * clips mid-glyph at the screen edge. Parallax scrolls with [pageProgress] in proportion to
 * title width plus [trailingSlack] (and at least the clipped overflow, so long brands fully
 * reveal — including the trailing bleed — by the last pane).
 *
 * Colour: optional [brush] (horizontal album / accent gradients). When [brush] is null, paints
 * solid [color] — **white by default** so dark immersive hubs keep contrast over album washes.
 * Pass [MetroTheme] `primaryText` explicitly when a light-theme hub needs theme ink instead.
 */
@Composable
fun MetroPanoramaBrandTitle(
    text: String,
    pageProgress: Float,
    pageCount: Int,
    modifier: Modifier = Modifier,
    brush: Brush? = null,
    color: Color = Color.White,
    trailingSlack: Dp = 0.dp,
) {
    val density = LocalDensity.current
    val fontFamily = MetroTheme.fontFamily
    val fill = brush ?: SolidColor(color)
    val style = remember(fontFamily, fill) {
        MetroPanoramaBrandStyle.copy(fontFamily = fontFamily, brush = fill)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .padding(bottom = 4.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        val measurer = rememberTextMeasurer()
        val availableWidthPx = with(density) {
            (maxWidth - MetroDimens.ScreenHorizontalMargin).toPx()
        }
        val trailingSlackPx = with(density) { trailingSlack.toPx() }
        val textWidthPx = remember(measurer, density, fontFamily, text) {
            measurer.measure(
                text = text,
                style = MetroPanoramaBrandStyle.copy(fontFamily = fontFamily),
                softWrap = false,
                maxLines = 1,
                density = density,
            ).size.width.toFloat()
        }
        val brandWidthPx = textWidthPx + trailingSlackPx
        val travelPx = metroPanoramaBrandTravelPx(
            brandWidthPx = brandWidthPx,
            availableWidthPx = availableWidthPx,
            pageCount = pageCount,
        )
        val lastPage = (pageCount - 1).coerceAtLeast(1)
        val progress = pageProgress.coerceIn(0f, lastPage.toFloat()) / lastPage
        val brandOffsetPx = (progress * travelPx).roundToInt()

        BasicText(
            text = text,
            style = style,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .offset { IntOffset(-brandOffsetPx, 0) }
                .padding(start = MetroDimens.ScreenHorizontalMargin, end = trailingSlack)
                .wrapContentWidth(align = Alignment.Start, unbounded = true),
        )
    }
}

/**
 * Horizontal travel for the brand title across a panorama swipe.
 *
 * Scales with [brandWidthPx] so longer titles move farther. Never less than the clipped
 * overflow (`brandWidth − availableWidth`) so overflowing glyphs still fully reveal.
 */
internal fun metroPanoramaBrandTravelPx(
    brandWidthPx: Float,
    availableWidthPx: Float,
    pageCount: Int,
    widthFraction: Float = MetroPanoramaBrandParallaxWidthFraction,
): Float {
    if (pageCount <= 1) return 0f
    val hiddenPx = (brandWidthPx - availableWidthPx).coerceAtLeast(0f)
    val lengthTravel = brandWidthPx * widthFraction
    return maxOf(hiddenPx, lengthTravel)
}

/** Shared ExtraLight 96sp face for panoramic brand titles. */
val MetroPanoramaBrandStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.ExtraLight,
    fontSize = 96.sp,
    lineHeight = 96.sp,
    letterSpacing = (-1).sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 120)
@Composable
private fun MetroPanoramaBrandTitlePreview() {
    MetroTheme(darkTheme = true) {
        MetroPanoramaBrandTitle(
            text = "metro music",
            pageProgress = 0f,
            pageCount = 4,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 120)
@Composable
private fun MetroPanoramaBrandTitleGradientPreview() {
    MetroTheme(darkTheme = true) {
        MetroPanoramaBrandTitle(
            text = "music",
            pageProgress = 0f,
            pageCount = 1,
            brush = Brush.horizontalGradient(
                listOf(
                    Color(0xFFFF8A00),
                    Color(0xFFFF2D55),
                    Color(0xFFAF52DE),
                ),
            ),
        )
    }
}
