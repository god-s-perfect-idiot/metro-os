package com.metro.clock.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroDimens

private enum class DurationField { Hours, Minutes, Seconds }

/**
 * Hours | minutes | seconds accent tiles — same language as the alarm time picker,
 * without a nested app title or app bar (those live on the host surface).
 */
@Composable
fun DurationTiles(
    hours: Int,
    minutes: Int,
    seconds: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    padForAppBar: Boolean = true,
) {
    var activeField by remember { mutableStateOf<DurationField?>(null) }
    val hoursValue = hours.coerceIn(0, 23)
    val minutesValue = minutes.coerceIn(0, 59)
    val secondsValue = seconds.coerceIn(0, 59)

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (interactive) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { activeField = null },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 8.dp,
                    bottom = if (padForAppBar) MetroAppBarDefaults.BarHeight + 8.dp else 8.dp,
                    start = MetroDimens.ScreenHorizontalMargin,
                    end = MetroDimens.ScreenHorizontalMargin,
                ),
        ) {
            val tileSize = ((maxWidth - TileGap * 2) / 3).coerceAtLeast(TileMinSize)
            val visibleRows = ((maxHeight / tileSize).toInt()).coerceAtLeast(3).let { rows ->
                if (rows % 2 == 0) (rows - 1).coerceAtLeast(3) else rows
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(TileGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimeFieldColumn(
                    label = hoursValue.toString(),
                    flipIndex = 0,
                    expanded = interactive && activeField == DurationField.Hours,
                    onToggle = {
                        if (!interactive) return@TimeFieldColumn
                        activeField =
                            if (activeField == DurationField.Hours) null else DurationField.Hours
                    },
                    values = (0..23).toList(),
                    selectedIndex = hoursValue,
                    format = { it.toString() },
                    onSelectedIndex = onHoursChange,
                    looping = true,
                    expandBelow = false,
                    tileSize = tileSize,
                    visibleRows = visibleRows,
                    modifier = Modifier.weight(1f),
                )
                TimeFieldColumn(
                    label = "%02d".format(minutesValue),
                    flipIndex = 1,
                    expanded = interactive && activeField == DurationField.Minutes,
                    onToggle = {
                        if (!interactive) return@TimeFieldColumn
                        activeField =
                            if (activeField == DurationField.Minutes) null else DurationField.Minutes
                    },
                    values = (0..59).toList(),
                    selectedIndex = minutesValue,
                    format = { "%02d".format(it) },
                    onSelectedIndex = onMinutesChange,
                    looping = true,
                    expandBelow = false,
                    tileSize = tileSize,
                    visibleRows = visibleRows,
                    modifier = Modifier.weight(1f),
                )
                TimeFieldColumn(
                    label = "%02d".format(secondsValue),
                    flipIndex = 2,
                    expanded = interactive && activeField == DurationField.Seconds,
                    onToggle = {
                        if (!interactive) return@TimeFieldColumn
                        activeField =
                            if (activeField == DurationField.Seconds) null else DurationField.Seconds
                    },
                    values = (0..59).toList(),
                    selectedIndex = secondsValue,
                    format = { "%02d".format(it) },
                    onSelectedIndex = onSecondsChange,
                    looping = true,
                    expandBelow = false,
                    tileSize = tileSize,
                    visibleRows = visibleRows,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
