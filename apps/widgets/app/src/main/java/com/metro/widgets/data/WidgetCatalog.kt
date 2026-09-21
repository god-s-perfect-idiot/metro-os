package com.metro.widgets.data

/**
 * Widget footprints on the catalog grid (4 columns).
 * **TwoByFour** = 2 rows × 4 columns (Start wide / launcher `4×2`).
 */
enum class WidgetTileSize(
    val colSpan: Int,
    val rowSpan: Int,
) {
    OneByOne(1, 1),
    TwoByTwo(2, 2),
    TwoByFour(4, 2),
}

enum class WidgetKind(
    val id: String,
    val title: String,
    val size: WidgetTileSize,
    val gridCol: Int,
    val gridRow: Int,
    /** Start hides titles on 1×1; Time wide face is title-free like the reference clock. */
    val showTitle: Boolean,
) {
    Time(
        id = "time",
        title = "time",
        size = WidgetTileSize.TwoByFour,
        gridCol = 0,
        gridRow = 0,
        showTitle = false,
    ),
    Battery(
        id = "battery",
        title = "battery",
        size = WidgetTileSize.OneByOne,
        gridCol = 0,
        gridRow = 2,
        showTitle = false,
    ),
    StorageSense(
        id = "storage_sense",
        title = "storage sense",
        size = WidgetTileSize.TwoByFour,
        gridCol = 0,
        gridRow = 3,
        showTitle = true,
    ),
}

object WidgetCatalog {
    val tiles: List<WidgetKind> = WidgetKind.entries

    const val COLUMNS = 4
}
