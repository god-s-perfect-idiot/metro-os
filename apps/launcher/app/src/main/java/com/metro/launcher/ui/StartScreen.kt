package com.metro.launcher.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.dp
import com.metro.launcher.data.DisplayTile
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroSystemIconType

/**
 * Start menu — tile grid on black (4 columns default; 6 when show more columns is on).
 * Reference: references/images/start_dark_blue.png
 */
@Composable
fun StartScreen(
    tiles: List<DisplayTile>,
    onTileClick: (DisplayTile) -> Unit,
    onTileLongPress: (DisplayTile) -> Unit,
    onOpenAppList: () -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = TILE_GRID_COLUMNS,
    editMode: Boolean = false,
    editingTile: DisplayTile? = null,
    onDismissEdit: () -> Unit = {},
    onResize: () -> Unit = {},
    onUnpin: () -> Unit = {},
    onDragLayout: (List<PlacedTile>) -> Unit = {},
    onReorderCommit: () -> Unit = {},
    enterWaveKey: Int = 0,
    consumedEnterWaveKey: Int = 0,
) {
    // verticalScroll consumes blank taps, so edit-mode dismiss must live on this surface —
    // not only on the dim scrim behind the grid (which never receives those events).
    val editDismissInteraction = remember { MutableInteractionSource() }
    val scrollState = rememberScrollState()
    val viewConfiguration = LocalViewConfiguration.current
    val fastPressConfiguration = remember(viewConfiguration) {
        object : ViewConfiguration by viewConfiguration {
            override val longPressTimeoutMillis: Long = TILE_DRAG_HOLD_MS
        }
    }
    val arrowRowHeight by animateDpAsState(
        targetValue = if (editMode) 0.dp else START_ARROW_ROW_HEIGHT,
        animationSpec = tween(TILE_EDIT_VISUAL_MS, easing = FastOutSlowInEasing),
        label = "startArrowRowHeight",
    )
    val arrowBottomPadding by animateDpAsState(
        targetValue = if (editMode) 0.dp else START_BOTTOM_SCROLL_PADDING,
        animationSpec = tween(TILE_EDIT_VISUAL_MS, easing = FastOutSlowInEasing),
        label = "startArrowBottomPadding",
    )
    val arrowAlpha by animateFloatAsState(
        targetValue = if (editMode) 0f else 1f,
        animationSpec = tween(TILE_EDIT_VISUAL_MS, easing = FastOutSlowInEasing),
        label = "startArrowAlpha",
    )
    CompositionLocalProvider(LocalViewConfiguration provides fastPressConfiguration) {
        // Never toggle verticalScroll.enabled mid-gesture — that cancels the tile's
        // pointerInput and forces a second drag. Tile gestures consume the pointer instead.
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .verticalScroll(scrollState)
                .then(
                    if (editMode) {
                        Modifier.clickable(
                            interactionSource = editDismissInteraction,
                            indication = null,
                            onClick = onDismissEdit,
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            TileGrid(
                tiles = tiles,
                onTileClick = onTileClick,
                onTileLongPress = onTileLongPress,
                columns = columns,
                editMode = editMode,
                activeTile = editingTile,
                onDismissEdit = onDismissEdit,
                onResize = onResize,
                onUnpin = onUnpin,
                onDragLayout = onDragLayout,
                onReorderCommit = onReorderCommit,
                enterWaveKey = enterWaveKey,
                consumedEnterWaveKey = consumedEnterWaveKey,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = TileChrome.forColumns(columns).horizontalPadding,
                        end = TileChrome.forColumns(columns).horizontalPadding,
                    ),
                horizontalAlignment = Alignment.End,
            ) {
                Box(
                    modifier = Modifier
                        .height(arrowRowHeight)
                        .clipToBounds(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    MetroCircleIconButton(
                        type = MetroSystemIconType.Forward,
                        onClick = onOpenAppList,
                        size = START_ARROW_ROW_HEIGHT,
                        contentDescription = "app list",
                        enabled = !editMode,
                        modifier = Modifier.alpha(arrowAlpha),
                    )
                }
                Spacer(modifier = Modifier.height(arrowBottomPadding))
            }
        }
    }
}
