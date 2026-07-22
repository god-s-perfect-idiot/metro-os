package com.metro.settings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metro.settings.R
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

@Composable
fun AccentPickerScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    val accents = MetroAccentPalette.all
    var exiting by remember { mutableStateOf(false) }

    // Swallow Back while the outro flip wave runs so we don't pop mid-animation.
    BackHandler(enabled = exiting) { }

    LaunchedEffect(exiting) {
        if (!exiting) return@LaunchedEffect
        delay(metroDiagonalFlipWaveDurationMs(accents.size, AccentGridColumns))
        state.open(SettingsRoute.StartTheme)
    }

    Column(modifier = modifier.fillMaxSize()) {
        MetroText(
            text = stringResource(R.string.settings_accents_title).uppercase(),
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
                        AccentSwatch(
                            option = option,
                            size = tileSize,
                            selected = MetroAccentPalette.normalizeHex(state.accentHex) ==
                                MetroAccentPalette.normalizeHex(option.hex),
                            enabled = !exiting,
                            onClick = {
                                state.applyAccentHex(option.hex)
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
private fun AccentSwatch(
    option: MetroAccentOption,
    size: Dp,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val color = Color(option.colorArgb)
    Box(
        modifier = Modifier
            .size(size)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(3.dp, MetroTheme.colors.primaryText)
                } else {
                    Modifier
                },
            )
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
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            MetroText(
                text = "✓",
                style = MetroTextStyle.SectionHeader,
                color = Color.White,
            )
        }
    }
}
