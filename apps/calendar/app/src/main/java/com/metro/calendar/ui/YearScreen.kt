package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.metro.system.MetroPreferences
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun YearScreen(
    year: Int,
    months: List<Pair<Int, String>>,
    events: List<CalendarEvent>,
    onSelectMonth: (Int) -> Unit,
    zoneId: ZoneId = ZoneId.systemDefault(),
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp),
    ) {
        item(key = "year-header") {
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(months, key = { it.first }) { (month, label) ->
            val monthEvents = events.filter { event ->
                val eventDay = CalendarLogic.epochDayFromMillis(event.startMillis, zoneId)
                val eventDate = LocalDate.ofEpochDay(eventDay)
                eventDate.year == year && eventDate.monthValue == month
            }
            YearMonthRow(
                label = label,
                eventColors = monthEvents.take(3).map { it.calendarColorHex },
                onClick = { onSelectMonth(month) },
            )
        }
        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}

@Composable
private fun YearMonthRow(
    label: String,
    eventColors: List<String>,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroText(
            text = label,
            style = MetroTextStyle.ListItemTitle,
            modifier = Modifier.weight(1f),
        )
        eventColors.forEach { colorHex ->
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(width = 12.dp, height = 4.dp)
                    .background(MetroPreferences.parseAccentHex(colorHex)),
            )
        }
    }
}
