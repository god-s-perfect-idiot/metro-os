package com.metro.launcher.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.metro.ui.MetroFontFamily

/**
 * Live-tile face typography.
 *
 * Tile content sits one step below the shared `MetroTextStyle` list/body scale: a 2×2 face is
 * ~99dp of usable width inside an 8dp inset and still has to carry a title line, a peek line,
 * and a footer, so the page-level 18/22sp roles read oversized inside a tile.
 */
internal object TileTextStyles {
    /** Leading line of a live face — peek sender, agenda event title. */
    val Title = TextStyle(
        fontFamily = MetroFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    /** Secondary peek lines, agenda detail lines, music metadata, bottom-left app title. */
    val Body = TextStyle(
        fontFamily = MetroFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    )

    /** Weekday label beside the agenda date numeral. */
    val DayLabel = TextStyle(
        fontFamily = MetroFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    )
}

/** [BasicText] in a [TileTextStyles] role, tinted to the tile's content color. */
@Composable
internal fun TileText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color),
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
    )
}
