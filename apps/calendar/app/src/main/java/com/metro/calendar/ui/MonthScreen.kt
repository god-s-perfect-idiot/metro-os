package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.MonthGridCell
import com.metro.system.MetroPreferences
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import java.time.LocalDate
import java.time.ZoneId

private val Weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun MonthScreen(
    epochDay: Long,
    grid: List<MonthGridCell>,
    selectedDayEvents: List<CalendarEvent>,
    onSelectDay: (Long) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    zoneId: ZoneId = ZoneId.systemDefault(),
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 12.dp),
    ) {
        item(key = "month-header") {
            Spacer(modifier = Modifier.height(4.dp))
        }

        item(key = "weekday-row") {
            Row(modifier = Modifier.fillMaxWidth()) {
                Weekdays.forEach { label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        MetroText(
                            text = label,
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText,
                        )
                    }
                }
            }
        }

        item(key = "month-grid") {
            Column {
                grid.chunked(7).forEachIndexed { rowIndex, week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { cell ->
                            MonthCell(
                                cell = cell,
                                onSelect = { onSelectDay(cell.epochDay) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (rowIndex < 5) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MetroTheme.colors.secondaryText.copy(alpha = 0.3f)),
                        )
                    }
                }
            }
        }

        if (selectedDayEvents.isNotEmpty()) {
            item(key = "selected-day-header") {
                MetroText(
                    text = CalendarLogic.dateHeaderLabel(epochDay, zoneId),
                    style = MetroTextStyle.SectionHeader,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
            }
            items(selectedDayEvents, key = { "sel-${it.id}-${it.startMillis}" }) { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onEventClick(event) })
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetroText(
                        text = CalendarLogic.formatEventTime(event),
                        style = MetroTextStyle.ListItemSubtitle,
                        modifier = Modifier.width(64.dp),
                    )
                    MetroText(
                        text = event.title,
                        style = MetroTextStyle.ListItemTitle,
                        color = MetroPreferences.parseAccentHex(event.calendarColorHex),
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}

@Composable
private fun MonthCell(
    cell: MonthGridCell,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = when {
        !cell.inCurrentMonth -> MetroTheme.colors.secondaryText.copy(alpha = 0.4f)
        cell.isSelected || cell.isToday -> MetroTheme.colors.primaryText
        else -> MetroTheme.colors.primaryText
    }
    val borderColor = if (cell.isSelected) MetroTheme.colors.accent else Color.Transparent

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .border(0.5.dp, MetroTheme.colors.secondaryText.copy(alpha = 0.3f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect,
            )
            .then(
                if (cell.isSelected) Modifier.border(1.dp, borderColor) else Modifier,
            )
            .padding(4.dp),
    ) {
        MetroText(
            text = cell.dayOfMonth.toString(),
            style = MetroTextStyle.ListItemSubtitle,
            color = textColor,
            modifier = Modifier.align(Alignment.TopStart),
        )
        if (cell.isToday) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 18.dp)
                    .width(16.dp)
                    .height(2.dp)
                    .background(MetroTheme.colors.accent),
            )
        }
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            cell.eventColors.forEach { colorHex ->
                Box(
                    modifier = Modifier
                        .size(width = 8.dp, height = 3.dp)
                        .background(MetroPreferences.parseAccentHex(colorHex)),
                )
            }
        }
    }
}
