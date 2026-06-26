package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.HourSlot
import com.metro.system.MetroPreferences
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun DayScreen(
    epochDay: Long,
    allDayEvents: List<CalendarEvent>,
    hourSlots: List<HourSlot>,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasEvents = allDayEvents.isNotEmpty() || hourSlots.any { it.events.isNotEmpty() }

    if (!hasEvents) {
        MetroEmptyState(
            message = stringResource(R.string.no_events),
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp),
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        if (allDayEvents.isNotEmpty()) {
            items(allDayEvents, key = { "allday-${it.id}-${it.startMillis}" }) { event ->
                MetroText(
                    text = event.title,
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroPreferences.parseAccentHex(event.calendarColorHex),
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable(onClick = { onEventClick(event) }),
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        items(hourSlots, key = { "hour-${it.hour}" }) { slot ->
            HourRow(slot = slot, onEventClick = onEventClick)
        }
        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}

@Composable
private fun HourRow(
    slot: HourSlot,
    onEventClick: (CalendarEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 4.dp),
    ) {
        MetroText(
            text = slot.label,
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.width(56.dp).padding(top = 2.dp),
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(48.dp)
                .background(MetroTheme.colors.secondaryText.copy(alpha = 0.4f)),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            if (slot.events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MetroTheme.colors.secondaryText.copy(alpha = 0.25f)),
                )
            } else {
                slot.events.forEach { event ->
                    val color = MetroPreferences.parseAccentHex(event.calendarColorHex)
                    MetroText(
                        text = "${CalendarLogic.formatEventTime(event)}  ${event.title}",
                        style = MetroTextStyle.ListItemTitle,
                        color = color,
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .clickable(onClick = { onEventClick(event) }),
                    )
                }
            }
        }
    }
}
