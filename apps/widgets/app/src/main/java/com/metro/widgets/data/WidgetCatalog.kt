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
    /** Start hides titles on 1×1; faces own their chrome. */
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
    Notifier(
        id = "notifier",
        title = "notifier",
        size = WidgetTileSize.TwoByTwo,
        gridCol = 1,
        gridRow = 2,
        showTitle = false,
    ),
    AnalogClock(
        id = "analog_clock",
        title = "clock",
        size = WidgetTileSize.OneByOne,
        gridCol = 3,
        gridRow = 2,
        showTitle = false,
    ),
    Torch(
        id = "torch",
        title = "torch",
        size = WidgetTileSize.OneByOne,
        gridCol = 0,
        gridRow = 3,
        showTitle = false,
    ),
    Lock(
        id = "lock",
        title = "lock",
        size = WidgetTileSize.OneByOne,
        gridCol = 3,
        gridRow = 3,
        showTitle = false,
    ),
}

object WidgetCatalog {
    val tiles: List<WidgetKind> = WidgetKind.entries

    const val COLUMNS = 4
}
