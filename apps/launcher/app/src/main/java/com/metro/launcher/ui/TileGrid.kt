package com.metro.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metro.launcher.data.DisplayTile
import com.metro.launcher.data.PinnedTileSize
import com.metro.system.MetroTileContract
import kotlin.math.min
import com.metro.ui.MetroColors
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import kotlin.math.max

const val TILE_GRID_COLUMNS = 4
val TILE_GRID_GAP = 8.dp
val TILE_GRID_PADDING = 12.dp
private val TILE_CONTENT_INSET = 8.dp
private val TILE_SMALL_ICON_INSET = 10.dp

data class PlacedTile(
    val tile: DisplayTile,
    val col: Int,
    val row: Int,
)

fun layoutTilesOnGrid(tiles: List<DisplayTile>, columns: Int = TILE_GRID_COLUMNS): List<PlacedTile> {
    val occupied = mutableSetOf<Pair<Int, Int>>()
    val placed = mutableListOf<PlacedTile>()

    fun canPlace(col: Int, row: Int, colSpan: Int, rowSpan: Int): Boolean {
        if (col + colSpan > columns) return false
        for (r in row until row + rowSpan) {
            for (c in col until col + colSpan) {
                if ((c to r) in occupied) return false
            }
        }
        return true
    }

    fun mark(col: Int, row: Int, colSpan: Int, rowSpan: Int) {
        for (r in row until row + rowSpan) {
            for (c in col until col + colSpan) {
                occupied += c to r
            }
        }
    }

    tiles.forEach { tile ->
        val colSpan = tile.entry.size.colSpan
        val rowSpan = tile.entry.size.rowSpan
        var row = 0
        while (true) {
            var col = 0
            var found = false
            while (col <= columns - colSpan) {
                if (canPlace(col, row, colSpan, rowSpan)) {
                    mark(col, row, colSpan, rowSpan)
                    placed += PlacedTile(tile, col, row)
                    found = true
                    break
                }
                col++
            }
            if (found) break
            row++
        }
    }
    return placed
}

fun tilePixelSize(unit: Dp, colSpan: Int, rowSpan: Int, gap: Dp = TILE_GRID_GAP): Pair<Dp, Dp> {
    val width = unit * colSpan + gap * (colSpan - 1)
    val height = unit * rowSpan + gap * (rowSpan - 1)
    return width to height
}

fun gridContentHeight(unit: Dp, placed: List<PlacedTile>, gap: Dp = TILE_GRID_GAP): Dp {
    if (placed.isEmpty()) return unit
    val maxRow = placed.maxOf { it.row + it.tile.entry.size.rowSpan }
    return unit * maxRow + gap * max(0, maxRow - 1)
}

@Composable
fun TileGrid(
    tiles: List<DisplayTile>,
    onTileClick: (DisplayTile) -> Unit,
    onTileLongPress: (DisplayTile) -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean = false,
    activeTile: DisplayTile? = null,
    onDismissEdit: () -> Unit = {},
    onResize: () -> Unit = {},
    onUnpin: () -> Unit = {},
) {
    val placed = layoutTilesOnGrid(tiles)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val unit = (maxWidth - TILE_GRID_PADDING * 2 - TILE_GRID_GAP * (TILE_GRID_COLUMNS - 1)) / TILE_GRID_COLUMNS
        val contentHeight = gridContentHeight(unit, placed)

        if (editMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(onClick = onDismissEdit),
            )
        }

        Box(
            modifier = Modifier
                .padding(
                    start = TILE_GRID_PADDING,
                    end = TILE_GRID_PADDING,
                    top = 8.dp,
                )
                .size(width = maxWidth, height = contentHeight + 16.dp),
        ) {
            placed.forEach { placement ->
                val tile = placement.tile
                val isActive = editMode && activeTile?.entry?.packageName == tile.entry.packageName &&
                    activeTile.entry.tileId == tile.entry.tileId
                if (editMode && isActive) return@forEach

                val (tileWidth, tileHeight) = tilePixelSize(
                    unit,
                    tile.entry.size.colSpan,
                    tile.entry.size.rowSpan,
                )
                val x = (unit + TILE_GRID_GAP) * placement.col
                val y = (unit + TILE_GRID_GAP) * placement.row

                LauncherTileCell(
                    tile = tile,
                    width = tileWidth,
                    height = tileHeight,
                    editMode = editMode,
                    isActive = false,
                    onClick = {
                        if (editMode) onDismissEdit() else onTileClick(tile)
                    },
                    onLongClick = { if (!editMode) onTileLongPress(tile) },
                    onResize = onResize,
                    onUnpin = onUnpin,
                    modifier = Modifier.offset(x = x, y = y),
                )
            }
        }

        if (editMode && activeTile != null) {
            val activePlacement = placed.firstOrNull { placement ->
                placement.tile.entry.packageName == activeTile.entry.packageName &&
                    placement.tile.entry.tileId == activeTile.entry.tileId
            }
            if (activePlacement != null) {
                val (tileWidth, tileHeight) = tilePixelSize(
                    unit,
                    activePlacement.tile.entry.size.colSpan,
                    activePlacement.tile.entry.size.rowSpan,
                )
                val x = (unit + TILE_GRID_GAP) * activePlacement.col
                val y = (unit + TILE_GRID_GAP) * activePlacement.row
                Box(
                    modifier = Modifier
                        .padding(
                            start = TILE_GRID_PADDING,
                            end = TILE_GRID_PADDING,
                            top = 8.dp,
                        )
                        .size(width = maxWidth, height = contentHeight + 16.dp),
                ) {
                    LauncherTileCell(
                        tile = activePlacement.tile,
                        width = tileWidth,
                        height = tileHeight,
                        editMode = true,
                        isActive = true,
                        onClick = {},
                        onLongClick = {},
                        onResize = onResize,
                        onUnpin = onUnpin,
                        modifier = Modifier.offset(x = x, y = y),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LauncherTileCell(
    tile: DisplayTile,
    width: Dp,
    height: Dp,
    editMode: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onResize: () -> Unit,
    onUnpin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimmed = editMode && !isActive
    val iconSize = tileIconSize(width, height, tile.entry.size)
    val contentColor = MetroColors.tileContentColor(tile.backgroundColor)
    val photoGrid = tile.photoGrid
    val gridDimensions = MetroTileContract.photoGridDimensions(
        tile.entry.size.colSpan,
        tile.entry.size.rowSpan,
    )
    val showPhotoGrid = photoGrid != null && gridDimensions != null

    Box(
        modifier = modifier
            .size(width, height)
            .graphicsLayer {
                alpha = if (dimmed) 0.45f else 1f
                scaleX = when {
                    isActive -> 1.02f
                    dimmed -> 0.97f
                    else -> 1f
                }
                scaleY = when {
                    isActive -> 1.02f
                    dimmed -> 0.97f
                    else -> 1f
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showPhotoGrid) Modifier else Modifier
                        .background(tile.backgroundColor)
                        .padding(TILE_CONTENT_INSET),
                ),
        ) {
            val isSmall = tile.entry.size == PinnedTileSize.OneByOne
            when {
                showPhotoGrid -> {
                    val (columns, rows) = gridDimensions!!
                    PhotoGridTileContent(
                        cells = photoGrid!!.cells,
                        columns = columns,
                        rows = rows,
                        title = tile.title,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                isSmall -> {
                    MetroAppIcon(
                        packageName = tile.entry.packageName,
                        size = iconSize,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(TILE_SMALL_ICON_INSET),
                        contentDescription = tile.title,
                        fallbackLabel = tile.title,
                        fallbackColor = contentColor,
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            MetroAppIcon(
                                packageName = tile.entry.packageName,
                                size = iconSize,
                                modifier = Modifier.size(iconSize),
                                contentDescription = tile.title,
                                fallbackLabel = tile.title,
                                fallbackColor = contentColor,
                            )
                        }
                        MetroText(
                            text = tile.title,
                            style = MetroTextStyle.Body,
                            color = contentColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            tile.counter?.takeIf { it > 0 }?.let { count ->
                MetroText(
                    text = count.toString(),
                    style = MetroTextStyle.Body,
                    color = contentColor,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
        if (isActive) {
            val cornerOffset = TileCornerButtonSize / 2
            TileEditCornerButton(
                onClick = onUnpin,
                contentDescription = "unpin",
                unpin = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = cornerOffset, y = -cornerOffset),
            )
            TileEditCornerButton(
                onClick = onResize,
                contentDescription = "resize",
                resizeGlyph = resizeGlyphForTileSize(tile.entry.size),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = cornerOffset, y = cornerOffset),
            )
        }
    }
}

private fun tileIconSize(tileWidth: Dp, tileHeight: Dp, size: PinnedTileSize): Dp {
    val base = min(tileWidth.value, tileHeight.value)
    val content = base - TILE_CONTENT_INSET.value * 2
    return when (size) {
        PinnedTileSize.OneByOne -> (content - TILE_SMALL_ICON_INSET.value * 2).dp
        PinnedTileSize.TwoByTwo -> (content * 0.55f).dp
        PinnedTileSize.FourByTwo -> (content * 0.42f).dp
    }
}
