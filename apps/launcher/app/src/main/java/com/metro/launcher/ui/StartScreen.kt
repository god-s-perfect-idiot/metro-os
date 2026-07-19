package com.metro.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metro.launcher.data.DisplayTile
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroSystemIconType

private val StartBottomScrollPadding = 120.dp

/**
 * Start menu — 4-column tile grid on black.
 * Reference: references/images/start_dark_blue.png
 */
@Composable
fun StartScreen(
    tiles: List<DisplayTile>,
    onTileClick: (DisplayTile) -> Unit,
    onTileLongPress: (DisplayTile) -> Unit,
    onOpenAppList: () -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean = false,
    editingTile: DisplayTile? = null,
    onDismissEdit: () -> Unit = {},
    onResize: () -> Unit = {},
    onUnpin: () -> Unit = {},
) {
    // verticalScroll consumes blank taps, so edit-mode dismiss must live on this surface —
    // not only on the dim scrim behind the grid (which never receives those events).
    val editDismissInteraction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
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
            editMode = editMode,
            activeTile = editingTile,
            onDismissEdit = onDismissEdit,
            onResize = onResize,
            onUnpin = onUnpin,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TILE_GRID_PADDING,
                    end = TILE_GRID_PADDING,
                    top = TILE_GRID_GAP,
                    bottom = StartBottomScrollPadding,
                ),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (!editMode) {
                MetroCircleIconButton(
                    type = MetroSystemIconType.Forward,
                    onClick = onOpenAppList,
                    size = 64.dp,
                    contentDescription = "app list",
                )
            }
        }
    }
}
