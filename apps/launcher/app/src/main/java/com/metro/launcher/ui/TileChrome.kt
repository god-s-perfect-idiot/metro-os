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
 * [Dense] is Settings → show more columns (6-column / 3 medium across): same side
 * gutters and inter-tile gap as Standard, with slightly calmer glyphs and smaller
 * titles for the denser cell size.
 */
data class TileChrome(
    /** Start screen left gutter outside the tile grid. */
    val horizontalPadding: Dp,
    /** Start screen right gutter. Matches [horizontalPadding] for a balanced Start. */
    val endPadding: Dp,
    /** Inter-tile gutter — same on 4-col and 6-col Start. */
    val gap: Dp,
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
        /** 4-column Start — default density metrics. */
        val Standard = TileChrome(
            horizontalPadding = 12.dp,
            endPadding = 12.dp,
            gap = 8.dp,
            contentInset = 8.dp,
            // Slightly fuller 1×1 glyphs than the legacy 10.dp inset.
            smallIconInset = 6.dp,
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
         * 6-column Start — same gap as Standard, slightly tighter side gutters so
         * three mediums breathe without looking edge-clamped. Icons fill more of each
         * smaller cell; titles stay a step smaller for readability.
         */
        val Dense = TileChrome(
            horizontalPadding = 8.dp,
            endPadding = 8.dp,
            gap = Standard.gap,
            contentInset = 4.dp,
            smallIconInset = 2.dp,
            mediumIconFraction = 0.70f,
            wideIconFraction = 0.58f,
            titleSp = 14f,
            titleLineHeightSp = 17f,
            titlePaddingH = 4.dp,
            titlePaddingV = 3.dp,
            liveTitleSp = 16f,
            liveTitleLineHeightSp = 19f,
            liveBodySp = 13f,
            liveBodyLineHeightSp = 16f,
            agendaDateMediumSp = 24f,
            agendaDateWideSp = 30f,
        )

        fun forColumns(columns: Int): TileChrome =
            if (columns >= TILE_GRID_COLUMNS_EXPANDED) Dense else Standard
    }
}

val LocalTileChrome = compositionLocalOf { TileChrome.Standard }

/** Naked tile counter text — never shows `99+`; max two digits. */
internal fun tileNotificationDisplayCount(count: Int): String =
    count.coerceAtMost(99).toString()
