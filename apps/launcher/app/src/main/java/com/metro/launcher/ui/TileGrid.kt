package com.metro.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.launcher.R
import com.metro.launcher.data.DisplayTile
import com.metro.launcher.data.PinnedTileSize
import com.metro.system.MetroTileAgenda
import com.metro.system.MetroTileContract
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import com.metro.ui.MetroColors
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTransitions
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val MESSAGING_PACKAGE = "com.metro.messaging"

const val TILE_GRID_COLUMNS = 4
val TILE_GRID_GAP = 8.dp
val TILE_GRID_PADDING = 12.dp
private val TILE_CONTENT_INSET = 8.dp
private val TILE_SMALL_ICON_INSET = 10.dp
/** Duration for tile resize / magnet reflow — matches Metro page transition timing. */
private const val TILE_RESIZE_MS = 300
/** How long each live-tile face stays visible before the next 600ms flip. */
private const val TILE_FLIP_HOLD_MS = 5_000L
/** Max initial stagger so neighboring live tiles don't flip in sync. */
private const val TILE_FLIP_STAGGER_MAX_MS = 4_000L
/** Per-cycle hold jitter (±) so tiles stay desynced over time. */
private const val TILE_FLIP_HOLD_JITTER_MS = 1_200L
/** Camera distance multiplier so rotationX reads as a 3D flip, not a squash. */
private const val TILE_FLIP_CAMERA_DISTANCE = 16f
private val TileResizeAnimation: AnimationSpec<Dp> = tween(
    durationMillis = TILE_RESIZE_MS,
    easing = FastOutSlowInEasing,
)
private val TileFlipHalfAnimation = tween<Float>(
    durationMillis = MetroTransitions.TileFlipMs / 2,
    easing = FastOutSlowInEasing,
)

private data class AnimatedTileBounds(
    val width: Dp,
    val height: Dp,
    val x: Dp,
    val y: Dp,
)

@Composable
private fun rememberAnimatedTileBounds(
    width: Dp,
    height: Dp,
    x: Dp,
    y: Dp,
    labelPrefix: String,
): AnimatedTileBounds {
    val animatedWidth by animateDpAsState(
        targetValue = width,
        animationSpec = TileResizeAnimation,
        label = "${labelPrefix}Width",
    )
    val animatedHeight by animateDpAsState(
        targetValue = height,
        animationSpec = TileResizeAnimation,
        label = "${labelPrefix}Height",
    )
    val animatedX by animateDpAsState(
        targetValue = x,
        animationSpec = TileResizeAnimation,
        label = "${labelPrefix}X",
    )
    val animatedY by animateDpAsState(
        targetValue = y,
        animationSpec = TileResizeAnimation,
        label = "${labelPrefix}Y",
    )
    return AnimatedTileBounds(animatedWidth, animatedHeight, animatedX, animatedY)
}

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

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Edit corner buttons sit half outside the tile; keep enough inset so the discs
        // are not clipped by the scroll viewport or screen edge.
        val cornerOverhang = TileCornerButtonSize / 2
        val horizontalPad = if (editMode) {
            maxOf(TILE_GRID_PADDING, cornerOverhang)
        } else {
            TILE_GRID_PADDING
        }
        val topPad = if (editMode) maxOf(8.dp, cornerOverhang) else 8.dp
        val unit = (maxWidth - horizontalPad * 2 - TILE_GRID_GAP * (TILE_GRID_COLUMNS - 1)) /
            TILE_GRID_COLUMNS
        val contentHeight = gridContentHeight(unit, placed)
        val animatedContentHeight by animateDpAsState(
            targetValue = contentHeight + 16.dp + if (editMode) cornerOverhang else 0.dp,
            animationSpec = TileResizeAnimation,
            label = "tileGridHeight",
        )

        Box(
            modifier = Modifier
                .padding(
                    start = horizontalPad,
                    end = horizontalPad,
                    top = topPad,
                )
                .size(width = maxWidth, height = animatedContentHeight)
                .then(
                    if (editMode) {
                        Modifier
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable(onClick = onDismissEdit)
                    } else {
                        Modifier
                    },
                ),
        ) {
            placed.forEach { placement ->
                val tile = placement.tile
                val isActive = editMode && activeTile?.entry?.packageName == tile.entry.packageName &&
                    activeTile.entry.tileId == tile.entry.tileId
                if (editMode && isActive) return@forEach

                key(tile.entry.packageName, tile.entry.tileId) {
                    val (tileWidth, tileHeight) = tilePixelSize(
                        unit,
                        tile.entry.size.colSpan,
                        tile.entry.size.rowSpan,
                    )
                    val bounds = rememberAnimatedTileBounds(
                        width = tileWidth,
                        height = tileHeight,
                        x = (unit + TILE_GRID_GAP) * placement.col,
                        y = (unit + TILE_GRID_GAP) * placement.row,
                        labelPrefix = "tile",
                    )

                    LauncherTileCell(
                        tile = tile,
                        width = bounds.width,
                        height = bounds.height,
                        editMode = editMode,
                        isActive = false,
                        onClick = {
                            if (editMode) onDismissEdit() else onTileClick(tile)
                        },
                        onLongClick = { if (!editMode) onTileLongPress(tile) },
                        onResize = onResize,
                        onUnpin = onUnpin,
                        modifier = Modifier.offset(x = bounds.x, y = bounds.y),
                    )
                }
            }

            if (editMode && activeTile != null) {
                val activePlacement = placed.firstOrNull { placement ->
                    placement.tile.entry.packageName == activeTile.entry.packageName &&
                        placement.tile.entry.tileId == activeTile.entry.tileId
                }
                if (activePlacement != null) {
                    key(
                        activePlacement.tile.entry.packageName,
                        activePlacement.tile.entry.tileId,
                        "active",
                    ) {
                        val (tileWidth, tileHeight) = tilePixelSize(
                            unit,
                            activePlacement.tile.entry.size.colSpan,
                            activePlacement.tile.entry.size.rowSpan,
                        )
                        val bounds = rememberAnimatedTileBounds(
                            width = tileWidth,
                            height = tileHeight,
                            x = (unit + TILE_GRID_GAP) * activePlacement.col,
                            y = (unit + TILE_GRID_GAP) * activePlacement.row,
                            labelPrefix = "activeTile",
                        )
                        LauncherTileCell(
                            tile = activePlacement.tile,
                            width = bounds.width,
                            height = bounds.height,
                            editMode = true,
                            isActive = true,
                            onClick = {},
                            onLongClick = {},
                            onResize = onResize,
                            onUnpin = onUnpin,
                            modifier = Modifier.offset(x = bounds.x, y = bounds.y),
                        )
                    }
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
    val density = LocalDensity.current
    val floatSeed = remember(tile.entry.packageName, tile.entry.tileId) {
        tile.entry.packageName.hashCode() * 31 + tile.entry.tileId.hashCode()
    }
    val idleFloat = rememberTileIdleFloat(enabled = dimmed, seed = floatSeed)
    val floatTx = with(density) { idleFloat.offsetXDp.dp.toPx() }
    val floatTy = with(density) { idleFloat.offsetYDp.dp.toPx() }
    val iconSize = tileIconSize(width, height, tile.entry.size)
    val contentColor = MetroColors.tileContentColor(tile.backgroundColor)
    val photoGrid = tile.photoGrid
    val gridDimensions = MetroTileContract.photoGridDimensions(
        tile.entry.size.colSpan,
        tile.entry.size.rowSpan,
    )
    val showCyclePhoto = photoGrid != null && photoGrid.cycle && photoGrid.hasContent &&
        gridDimensions != null
    val showPhotoGrid = photoGrid != null && !photoGrid.cycle && gridDimensions != null
    val showPhotoContent = showCyclePhoto || showPhotoGrid
    val agenda = tile.agenda?.takeIf { it.hasContent }
    val showAgenda = agenda != null && !showPhotoContent &&
        tile.entry.size != PinnedTileSize.OneByOne
    val isSmall = tile.entry.size == PinnedTileSize.OneByOne
    val isMessaging = tile.entry.packageName == MESSAGING_PACKAGE
    val messagingUnread = tile.counter?.takeIf { it > 0 && isMessaging }
    // Medium/wide Messaging unread: wink glyph + large count (tile_yellow.jpg), not a corner badge.
    val showMessagingUnreadFace = messagingUnread != null && !isSmall &&
        !showPhotoContent && !showAgenda
    val canFlip = tile.hasFlipFace &&
        !showPhotoContent &&
        !showAgenda &&
        !showMessagingUnreadFace &&
        !isSmall &&
        !editMode

    // Outer box must not clip — edit corner buttons are centered on the tile vertices and
    // intentionally draw half outside the tile (WP8.1). Clip only the tile face below.
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
                translationX = floatTx
                translationY = floatTy
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        // Flip tiles keep a black void in the slot; the accent fill rides on the rotating
        // face so the Start-screen black shows through during the 3D flip.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .then(
                    when {
                        showPhotoContent -> Modifier
                        canFlip -> Modifier.background(MetroColors.DarkBackground)
                        else -> Modifier
                            .background(tile.backgroundColor)
                            .padding(TILE_CONTENT_INSET)
                    },
                ),
        ) {
            val frontFace: @Composable () -> Unit = {
                when {
                    showCyclePhoto -> {
                        CyclingPhotoTileContent(
                            cells = photoGrid!!.cells,
                            title = tile.title,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
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
                    showAgenda -> {
                        AgendaTileContent(
                            agenda = agenda!!,
                            wide = tile.entry.size == PinnedTileSize.FourByTwo,
                            contentColor = contentColor,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    showMessagingUnreadFace -> {
                        MessagingUnreadTileContent(
                            count = messagingUnread!!,
                            iconSize = iconSize,
                            contentColor = contentColor,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    isMessaging && isSmall -> {
                        MessagingGlyph(
                            unread = messagingUnread != null,
                            contentColor = contentColor,
                            contentDescription = tile.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(TILE_SMALL_ICON_INSET),
                        )
                    }
                    isMessaging -> {
                        MessagingIdleTileContent(
                            title = tile.title,
                            iconSize = iconSize,
                            contentColor = contentColor,
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
                        StaticIconTileContent(
                            packageName = tile.entry.packageName,
                            title = tile.title,
                            iconSize = iconSize,
                            contentColor = contentColor,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            val badgeCount = tile.counter?.takeIf {
                it > 0 && !showAgenda && !showMessagingUnreadFace
            }
            if (canFlip) {
                LiveTileFlipFace(
                    flipSeed = floatSeed,
                    faceColor = tile.backgroundColor,
                    front = frontFace,
                    back = {
                        NotificationPeekTileContent(
                            title = tile.backFaceTitle,
                            body = tile.backFaceBody,
                            footer = tile.title,
                            wide = tile.entry.size == PinnedTileSize.FourByTwo,
                            contentColor = contentColor,
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                    badge = badgeCount?.let { count ->
                        {
                            MetroText(
                                text = count.toString(),
                                style = MetroTextStyle.Body,
                                color = contentColor,
                                modifier = Modifier.align(Alignment.TopEnd),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                frontFace()
                badgeCount?.let { count ->
                    MetroText(
                        text = count.toString(),
                        style = MetroTextStyle.Body,
                        color = contentColor,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
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

/**
 * WP8.1 Calendar-style live tile face: event lines stacked from the top, an optional footer label
 * (app name) flush bottom-left, and a large bottom-right date badge (`Thu 15`).
 *
 * Wide (4×2) tiles show up to three lines (title, location, time) and let the title wrap; medium
 * (2×2) tiles drop the middle line and keep the title plus the trailing time/all-day line so the
 * badge stays legible.
 */
@Composable
private fun AgendaTileContent(
    agenda: MetroTileAgenda,
    wide: Boolean,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val lines = agenda.lines.filter { it.isNotBlank() }
    val shown = when {
        wide -> lines.take(3)
        lines.size <= 2 -> lines.take(2)
        else -> listOf(lines.first(), lines.last())
    }
    val footer = agenda.footer?.takeIf { it.isNotBlank() }
    val dayLabel = agenda.dayLabel?.takeIf { it.isNotBlank() }
    val dayNumber = agenda.dayNumber?.takeIf { it.isNotBlank() }

    Column(modifier = modifier) {
        shown.forEachIndexed { index, line ->
            MetroText(
                text = line,
                style = if (index == 0) MetroTextStyle.ListItemSubtitle else MetroTextStyle.Body,
                color = contentColor,
                maxLines = when {
                    wide && index == 0 -> 2
                    else -> 1
                },
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (footer != null) {
                MetroText(
                    text = footer,
                    style = MetroTextStyle.Body,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (dayLabel != null) {
                BasicText(
                    text = dayLabel,
                    style = TextStyle(
                        fontFamily = MetroFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        color = contentColor,
                    ),
                    modifier = Modifier.padding(end = 4.dp, bottom = if (wide) 6.dp else 4.dp),
                )
            }
            if (dayNumber != null) {
                BasicText(
                    text = dayNumber,
                    style = TextStyle(
                        fontFamily = MetroFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = if (wide) 44.sp else 34.sp,
                        color = contentColor,
                    ),
                )
            }
        }
    }
}

@Composable
private fun StaticIconTileContent(
    packageName: String,
    title: String,
    iconSize: Dp,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            MetroAppIcon(
                packageName = packageName,
                size = iconSize,
                modifier = Modifier.size(iconSize),
                contentDescription = title,
                fallbackLabel = title,
                fallbackColor = contentColor,
            )
        }
        MetroText(
            text = title,
            style = MetroTextStyle.Body,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Idle Messaging Start face: :-) bubble + app title. */
@Composable
private fun MessagingIdleTileContent(
    title: String,
    iconSize: Dp,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            MessagingGlyph(
                unread = false,
                contentColor = contentColor,
                contentDescription = title,
                modifier = Modifier.size(iconSize),
            )
        }
        MetroText(
            text = title,
            style = MetroTextStyle.Body,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Unread Messaging live-tile face: ;-) bubble + large count (`tile_yellow.jpg`).
 * No corner badge and no app-title footer — the count sits beside the glyph.
 */
@Composable
private fun MessagingUnreadTileContent(
    count: Int,
    iconSize: Dp,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val digits = count.toString().length
    val countSp = when {
        digits <= 1 -> iconSize.value * 0.78f
        digits == 2 -> iconSize.value * 0.62f
        digits == 3 -> iconSize.value * 0.48f
        else -> iconSize.value * 0.38f
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MessagingGlyph(
                unread = true,
                contentColor = contentColor,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
            BasicText(
                text = count.toString(),
                style = TextStyle(
                    fontFamily = MetroFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = countSp.sp,
                    color = contentColor,
                ),
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun MessagingGlyph(
    unread: Boolean,
    contentColor: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(
            if (unread) R.drawable.ic_system_messaging_unread else R.drawable.ic_system_messaging,
        ),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(contentColor),
        modifier = modifier,
    )
}

/**
 * WP8.1 notification / peek face: title + body stacked from the top, app name as footer.
 *
 * Wide (4×2) tiles wrap title and body across multiple lines so peek copy is readable instead of
 * single-line ellipsis; medium (2×2) stays single-line per field.
 */
@Composable
private fun NotificationPeekTileContent(
    title: String?,
    body: String?,
    footer: String,
    wide: Boolean,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val lines = buildList {
        title?.takeIf { it.isNotBlank() }?.let { add(it) }
        body?.takeIf { it.isNotBlank() }?.let { add(it) }
    }.let { all ->
        when {
            wide -> all.take(3)
            all.size <= 2 -> all
            else -> listOf(all.first(), all.last())
        }
    }

    Column(modifier = modifier) {
        lines.forEachIndexed { index, line ->
            val isTitle = index == 0
            MetroText(
                text = line,
                style = if (isTitle) MetroTextStyle.ListItemSubtitle else MetroTextStyle.Body,
                color = contentColor,
                maxLines = when {
                    !wide -> 1
                    // Sole peek field can fill the face above the footer.
                    lines.size == 1 -> 5
                    isTitle -> 2
                    else -> 4
                },
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        MetroText(
            text = footer,
            style = MetroTextStyle.Body,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 600ms vertical flip (around the horizontal center axis) between front (icon/title) and
 * back (notification peek) faces. The accent [faceColor] is painted on the rotating face so
 * the black tile slot behind it is revealed mid-flip. [flipSeed] drives a per-tile random
 * stagger so flips don't synchronize across the Start screen.
 */
@Composable
private fun LiveTileFlipFace(
    flipSeed: Int,
    faceColor: Color,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit,
    badge: (@Composable BoxScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    var showingBack by remember { mutableStateOf(false) }

    LaunchedEffect(flipSeed) {
        val rng = Random(flipSeed)
        delay(rng.nextLong(0L, TILE_FLIP_STAGGER_MAX_MS + 1))
        while (true) {
            val jitter = rng.nextLong(-TILE_FLIP_HOLD_JITTER_MS, TILE_FLIP_HOLD_JITTER_MS + 1)
            delay((TILE_FLIP_HOLD_MS + jitter).coerceAtLeast(2_500L))
            // Pivot about the tile center: 0° → +90° (edge-on), swap face, −90° → 0°.
            rotation.animateTo(90f, animationSpec = TileFlipHalfAnimation)
            showingBack = !showingBack
            rotation.snapTo(-90f)
            rotation.animateTo(0f, animationSpec = TileFlipHalfAnimation)
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationX = rotation.value
                transformOrigin = TransformOrigin(0.5f, 0.5f)
                cameraDistance = TILE_FLIP_CAMERA_DISTANCE * density
            }
            .background(faceColor)
            .padding(TILE_CONTENT_INSET),
    ) {
        if (showingBack) back() else front()
        badge?.invoke(this)
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

/**
 * Per-tile idle float used in edit mode for every non-active tile.
 * Amplitudes / periods are derived from [seed] so neighboring tiles drift independently.
 */
internal data class TileIdleFloatParams(
    val ampXDp: Float,
    val ampYDp: Float,
    val durationXMs: Int,
    val durationYMs: Int,
) {
    companion object {
        fun fromSeed(seed: Int): TileIdleFloatParams {
            val s = abs(seed)
            return TileIdleFloatParams(
                ampXDp = 3.5f + (s % 50) / 50f * 5f,
                ampYDp = 3f + ((s / 7) % 50) / 50f * 5f,
                durationXMs = 2200 + (s % 11) * 140,
                durationYMs = 2400 + ((s / 3) % 13) * 130,
            )
        }
    }
}

private data class TileIdleFloatState(
    val offsetXDp: Float,
    val offsetYDp: Float,
) {
    companion object {
        val Still = TileIdleFloatState(0f, 0f)
    }
}

@Composable
private fun rememberTileIdleFloat(enabled: Boolean, seed: Int): TileIdleFloatState {
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    LaunchedEffect(enabled, seed) {
        if (!enabled) {
            offsetX.snapTo(0f)
            offsetY.snapTo(0f)
            return@LaunchedEffect
        }
        val params = TileIdleFloatParams.fromSeed(seed)
        val phaseX = (abs(seed) % 100) / 100f
        val phaseY = ((abs(seed) / 11) % 100) / 100f
        offsetX.snapTo(params.ampXDp * (phaseX * 2f - 1f))
        offsetY.snapTo(params.ampYDp * (phaseY * 2f - 1f))

        coroutineScope {
            launch {
                var towardPositive = phaseX < 0.5f
                while (true) {
                    offsetX.animateTo(
                        if (towardPositive) params.ampXDp else -params.ampXDp,
                        animationSpec = tween(params.durationXMs, easing = FastOutSlowInEasing),
                    )
                    towardPositive = !towardPositive
                }
            }
            launch {
                var towardPositive = phaseY < 0.5f
                while (true) {
                    offsetY.animateTo(
                        if (towardPositive) params.ampYDp else -params.ampYDp,
                        animationSpec = tween(params.durationYMs, easing = FastOutSlowInEasing),
                    )
                    towardPositive = !towardPositive
                }
            }
        }
    }

    return if (enabled) {
        TileIdleFloatState(
            offsetXDp = offsetX.value,
            offsetYDp = offsetY.value,
        )
    } else {
        TileIdleFloatState.Still
    }
}
