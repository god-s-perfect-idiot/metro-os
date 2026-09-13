package com.metro.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metro.launcher.R
import com.metro.system.MetroAccentOption
import com.metro.system.MetroAccentPalette
import com.metro.ui.MetroDiagonalFlip
import com.metro.ui.MetroJumpListLogic
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroDiagonalFlipWaveDurationMs
import kotlinx.coroutines.delay

private val AccentGridGap = 8.dp
private val AccentGridHorizontalPadding = 8.dp
private const val AccentGridColumns = MetroJumpListLogic.GridColumns

/**
 * WP8.1 accent-grid color picker for a custom tile background — same chrome as Settings → accents.
 */
@Composable
fun TileColorPickerScreen(
    onColorSelected: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accents = MetroAccentPalette.all
    var exiting by remember { mutableStateOf(false) }
    var pendingHex by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = !exiting) { onClose() }
    BackHandler(enabled = exiting) { }

    LaunchedEffect(exiting, pendingHex) {
        if (!exiting) return@LaunchedEffect
        delay(metroDiagonalFlipWaveDurationMs(accents.size, AccentGridColumns))
        val hex = pendingHex
        if (hex != null) {
            onColorSelected(hex)
        } else {
            onClose()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        MetroText(
            text = stringResource(R.string.tile_customize_accents_title).uppercase(),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.primaryText,
            modifier = Modifier.padding(
                start = 12.dp,
                top = 8.dp,
                bottom = 16.dp,
            ),
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = AccentGridHorizontalPadding),
        ) {
            val tileSize = (maxWidth - AccentGridGap * (AccentGridColumns - 1)) / AccentGridColumns

            LazyVerticalGrid(
                columns = GridCells.Fixed(AccentGridColumns),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AccentGridGap),
                verticalArrangement = Arrangement.spacedBy(AccentGridGap),
                userScrollEnabled = false,
            ) {
                itemsIndexed(accents, key = { _, option -> option.hex }) { index, option ->
                    MetroDiagonalFlip(
                        cellIndex = index,
                        columns = AccentGridColumns,
                        exiting = exiting,
                    ) {
                        ColorSwatch(
                            option = option,
                            size = tileSize,
                            enabled = !exiting,
                            onClick = {
                                pendingHex = option.hex
                                exiting = true
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    option: MetroAccentOption,
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color(option.colorArgb))
            .semantics {
                role = Role.Button
                contentDescription = option.name
            }
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    )
}
