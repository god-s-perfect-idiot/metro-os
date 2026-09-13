package com.metro.launcher.data

import android.appwidget.AppWidgetManager

/**
 * Tile footprint on the Start grid (4 columns default; 6 when show more columns is on).
 * Reference: references/guides/blueprint.md
 */
enum class PinnedTileSize(
    val storageValue: String,
    val colSpan: Int,
    val rowSpan: Int,
) {
    OneByOne("1x1", 1, 1),
    TwoByTwo("2x2", 2, 2),
    FourByTwo("4x2", 4, 2),
    ;

    companion object {
        fun fromStorage(value: String?): PinnedTileSize = when (value) {
            "1x1", "small" -> OneByOne
            "2x2", "medium" -> TwoByTwo
            "4x2", "wide" -> FourByTwo
            else -> TwoByTwo
        }
    }
}

/** How the Start tile fill is resolved when the brush customize page is saved. */
enum class TileBackgroundMode(val storageValue: String) {
    /** Package / provider / branding defaults (existing Start behavior). */
    Default("default"),
    /** Force the system accent. */
    Accent("accent"),
    /** Fixed hex from the accent palette picker. */
    Custom("custom"),
    ;

    companion object {
        fun fromStorage(value: String?): TileBackgroundMode = when (value) {
            "accent" -> Accent
            "custom" -> Custom
            else -> Default
        }
    }
}

data class PinnedTileEntry(
    val packageName: String,
    val tileId: String = "primary",
    val size: PinnedTileSize = PinnedTileSize.OneByOne,
    /** Top-left grid column; assigned on first layout when null. */
    val gridCol: Int? = null,
    /** Top-left grid row; assigned on first layout when null. */
    val gridRow: Int? = null,
    val backgroundMode: TileBackgroundMode = TileBackgroundMode.Default,
    /** Accent-palette hex when [backgroundMode] is [TileBackgroundMode.Custom]. */
    val customBackgroundHex: String? = null,
    /** When true on 2×2 / 4×2 tiles, host an Android App Widget instead of the live face. */
    val useCustomWidget: Boolean = false,
    /** [android.content.ComponentName.flattenToString] of the selected App Widget provider. */
    val widgetProvider: String? = null,
    val appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
)

fun PinnedTileEntry.hasGridPosition(): Boolean = gridCol != null && gridRow != null

fun PinnedTileEntry.supportsCustomWidget(): Boolean =
    size == PinnedTileSize.TwoByTwo || size == PinnedTileSize.FourByTwo

fun PinnedTileEntry.hasActiveCustomWidget(): Boolean =
    useCustomWidget &&
        supportsCustomWidget() &&
        !widgetProvider.isNullOrBlank() &&
        appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID

object TileSizeCycle {
    fun nextSize(current: PinnedTileSize): PinnedTileSize = when (current) {
        PinnedTileSize.OneByOne -> PinnedTileSize.TwoByTwo
        PinnedTileSize.TwoByTwo -> PinnedTileSize.FourByTwo
        PinnedTileSize.FourByTwo -> PinnedTileSize.OneByOne
    }
}
