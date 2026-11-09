package com.metro.clock.ui

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.clock.data.AlarmLogic
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTheme

private enum class TimeField { Hour, Minute, Meridiem }

private val ScreenSideInset = MetroDimens.ScreenHorizontalMargin

@Composable
fun TimePickerScreen(
    hour24: Int,
    minute: Int,
    onConfirm: (hour24: Int, minute: Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (startHour12, startAm) = remember(hour24) { AlarmLogic.fromHour24(hour24) }
    var hour12 by remember { mutableIntStateOf(startHour12) }
    var minuteValue by remember { mutableIntStateOf(minute.coerceIn(0, 59)) }
    var isAm by remember { mutableStateOf(startAm) }
    var activeField by remember { mutableStateOf<TimeField?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { activeField = null },
            ),
    ) {
        MetroAppTitle(title = "TIME")

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 48.dp,
                    bottom = MetroAppBarDefaults.BarHeight + 8.dp,
                    start = ScreenSideInset,
                    end = ScreenSideInset,
                ),
        ) {
            val columnHeight = maxHeight
            val tileSize = ((maxWidth - TileGap * 2) / 3).coerceAtLeast(TileMinSize)
            val visibleRows = ((columnHeight / tileSize).toInt()).coerceAtLeast(3).let { rows ->
                if (rows % 2 == 0) (rows - 1).coerceAtLeast(3) else rows
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(TileGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimeFieldColumn(
                    label = hour12.toString(),
                    flipIndex = 0,
                    expanded = activeField == TimeField.Hour,
                    onToggle = {
                        activeField =
                            if (activeField == TimeField.Hour) null else TimeField.Hour
                    },
                    values = (1..12).toList(),
                    selectedIndex = hour12 - 1,
                    format = { value -> value.toString() },
                    onSelectedIndex = { hour12 = it + 1 },
                    looping = true,
                    expandBelow = false,
                    tileSize = tileSize,
                    visibleRows = visibleRows,
                    modifier = Modifier.weight(1f),
                )
                TimeFieldColumn(
                    label = "%02d".format(minuteValue),
                    flipIndex = 1,
                    expanded = activeField == TimeField.Minute,
                    onToggle = {
                        activeField =
                            if (activeField == TimeField.Minute) null else TimeField.Minute
                    },
                    values = (0..59).toList(),
                    selectedIndex = minuteValue,
                    format = { value -> "%02d".format(value) },
                    onSelectedIndex = { minuteValue = it },
                    looping = true,
                    expandBelow = false,
                    tileSize = tileSize,
                    visibleRows = visibleRows,
                    modifier = Modifier.weight(1f),
                )
                TimeFieldColumn(
                    label = if (isAm) "AM" else "PM",
                    flipIndex = 2,
                    expanded = activeField == TimeField.Meridiem,
                    onToggle = {
                        activeField =
                            if (activeField == TimeField.Meridiem) null else TimeField.Meridiem
                    },
                    values = listOf(0, 1),
                    selectedIndex = if (isAm) 0 else 1,
                    format = { value -> if (value == 0) "AM" else "PM" },
                    onSelectedIndex = { isAm = it == 0 },
                    looping = false,
                    expandBelow = true,
                    tileSize = tileSize,
                    visibleRows = 2,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Check,
                    label = "done",
                    onClick = {
                        onConfirm(AlarmLogic.toHour24(hour12, isAm), minuteValue)
                    },
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Close,
                    label = "cancel",
                    onClick = onCancel,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
