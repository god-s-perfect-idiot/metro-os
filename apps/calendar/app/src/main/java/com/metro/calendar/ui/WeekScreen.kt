package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.DayBucket
import com.metro.system.MetroPreferences
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun WeekScreen(
    buckets: List<DayBucket>,
    usingDemoData: Boolean,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (buckets.isEmpty()) {
        MetroEmptyState(
            message = stringResource(R.string.no_events),
            modifier = modifier,
        )
        return
    }

    val hasEvents = buckets.any { it.events.isNotEmpty() }

    if (!hasEvents && !usingDemoData) {
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
        if (usingDemoData) {
            item(key = "demo-banner") {
                MetroText(
                    text = stringResource(R.string.demo_data_banner),
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(bottom = 12.dp, top = 4.dp),
                )
            }
        }

        buckets.forEach { bucket ->
            item(key = "header-${bucket.epochDay}") {
                MetroText(
                    text = bucket.headerLabel,
                    style = MetroTextStyle.SectionHeader,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
            }
            if (bucket.events.isEmpty()) {
                item(key = "empty-${bucket.epochDay}") {
                    MetroText(
                        text = stringResource(R.string.no_events_day),
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            } else {
                items(bucket.events, key = { "${bucket.epochDay}-${it.id}-${it.startMillis}" }) { event ->
                    WeekEventRow(event = event, onClick = { onEventClick(event) })
                }
            }
        }
        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}

@Composable
private fun WeekEventRow(
    event: CalendarEvent,
    onClick: () -> Unit,
) {
    val eventColor = MetroPreferences.parseAccentHex(event.calendarColorHex)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroText(
            text = CalendarLogic.formatEventTime(event),
            style = MetroTextStyle.ListItemTitle,
            modifier = Modifier.width(80.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            MetroText(
                text = event.title,
                style = MetroTextStyle.ListItemTitle,
                color = eventColor,
            )
            MetroText(
                text = CalendarLogic.formatEventDuration(event),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
            )
        }
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(48.dp)
                .background(eventColor),
        )
    }
}
