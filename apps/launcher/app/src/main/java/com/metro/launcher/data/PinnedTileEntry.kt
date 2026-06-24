package com.metro.launcher.data

/**
 * Tile footprint on the 4-column Start grid.
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

data class PinnedTileEntry(
    val packageName: String,
    val tileId: String = "primary",
    val size: PinnedTileSize = PinnedTileSize.TwoByTwo,
)

object TileSizeCycle {
    fun nextSize(current: PinnedTileSize): PinnedTileSize = when (current) {
        PinnedTileSize.OneByOne -> PinnedTileSize.TwoByTwo
        PinnedTileSize.TwoByTwo -> PinnedTileSize.FourByTwo
        PinnedTileSize.FourByTwo -> PinnedTileSize.OneByOne
    }
}
