package com.metro.launcher.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.launcher.data.PinnedTileSize

/**
 * Start-tile icon / title metrics for the active grid density.
 *
 * [Standard] matches the default 4-column Start (2 medium tiles across).
 * [Dense] is Settings → show more columns (6-column / 3 medium across): tighter insets so
 * glyphs fill the smaller cells, and smaller titles so labels stay controlled like WP8.1.
 */
data class TileChrome(
    val contentInset: Dp,
    val smallIconInset: Dp,
    val mediumIconFraction: Float,
    val wideIconFraction: Float,
    /** Bottom-left app title on medium/wide static faces. */
    val titleSp: Float,
    val titleLineHeightSp: Float,
    val titlePaddingH: Dp,
    val titlePaddingV: Dp,
    /** Live-face leading line (peek sender, agenda title). */
    val liveTitleSp: Float,
    val liveTitleLineHeightSp: Float,
    /** Live-face secondary lines + footer. */
    val liveBodySp: Float,
    val liveBodyLineHeightSp: Float,
    /** Agenda date numeral on medium / wide. */
    val agendaDateMediumSp: Float,
    val agendaDateWideSp: Float,
) {
    val titleStyle: TextStyle
        get() = TileTextStyles.Body.copy(
            fontSize = titleSp.sp,
            lineHeight = titleLineHeightSp.sp,
        )

    val liveTitleStyle: TextStyle
        get() = TileTextStyles.Title.copy(
            fontSize = liveTitleSp.sp,
            lineHeight = liveTitleLineHeightSp.sp,
        )

    val liveBodyStyle: TextStyle
        get() = TileTextStyles.Body.copy(
            fontSize = liveBodySp.sp,
            lineHeight = liveBodyLineHeightSp.sp,
        )

    fun iconSize(tileWidth: Dp, tileHeight: Dp, size: PinnedTileSize): Dp {
        val base = minOf(tileWidth.value, tileHeight.value)
        val content = base - contentInset.value * 2
        return when (size) {
            PinnedTileSize.OneByOne -> (content - smallIconInset.value * 2).dp
            PinnedTileSize.TwoByTwo -> (content * mediumIconFraction).dp
            PinnedTileSize.FourByTwo -> (content * wideIconFraction).dp
        }
    }

    companion object {
        /** 4-column Start — keep these values unchanged for visual parity. */
        val Standard = TileChrome(
            contentInset = 8.dp,
            smallIconInset = 10.dp,
            mediumIconFraction = 0.55f,
            wideIconFraction = 0.42f,
            titleSp = 16f,
            titleLineHeightSp = 20f,
            titlePaddingH = 6.dp,
            titlePaddingV = 4.dp,
            liveTitleSp = 20f,
            liveTitleLineHeightSp = 24f,
            liveBodySp = 16f,
            liveBodyLineHeightSp = 20f,
            agendaDateMediumSp = 30f,
            agendaDateWideSp = 40f,
        )

        /**
         * 6-column Start — icons extend further into the cell; titles scale down so they
         * don't crowd the glyph (WP8.1 "show more tiles" density).
         */
        val Dense = TileChrome(
            contentInset = 5.dp,
            smallIconInset = 5.dp,
            mediumIconFraction = 0.62f,
            wideIconFraction = 0.50f,
            titleSp = 12f,
            titleLineHeightSp = 14f,
            titlePaddingH = 4.dp,
            titlePaddingV = 3.dp,
            liveTitleSp = 15f,
            liveTitleLineHeightSp = 18f,
            liveBodySp = 12f,
            liveBodyLineHeightSp = 15f,
            agendaDateMediumSp = 22f,
            agendaDateWideSp = 28f,
        )

        fun forColumns(columns: Int): TileChrome =
            if (columns >= TILE_GRID_COLUMNS_EXPANDED) Dense else Standard
    }
}

val LocalTileChrome = compositionLocalOf { TileChrome.Standard }
