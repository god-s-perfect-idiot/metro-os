package com.metro.widgets.data

/**
 * Maps catalog widgets to launcher pin payloads ([com.metro.system.MetroIntents.requestPinTile]).
 * Start storage uses `4x2` for catalog [WidgetTileSize.TwoByFour] (2 rows × 4 cols).
 */
object WidgetPinLogic {
    fun tileId(kind: WidgetKind): String = kind.id

    fun pinSizeStorageValue(kind: WidgetKind): String = when (kind.size) {
        WidgetTileSize.OneByOne -> "1x1"
        WidgetTileSize.TwoByTwo -> "2x2"
        WidgetTileSize.TwoByFour -> "4x2"
    }

    fun kindForTileId(tileId: String): WidgetKind? =
        WidgetKind.entries.firstOrNull { it.id == tileId }
}
