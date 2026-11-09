package com.metro.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.EventDraft
import com.metro.calendar.data.WritableCalendar
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroToggleSwitch

@Composable
fun NewEventScreen(
    draft: EventDraft,
    calendars: List<WritableCalendar>,
    onDraftChange: (EventDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var openPicker by remember { mutableStateOf<String?>(null) }
    val startOptions = remember { startTimeLabels() }
    val durationOptions = remember { durationLabels() }
    val calendarLabels = calendars.map { calendarLabel(it) }
    val selectedCalendarIndex = calendars.indexOfFirst { it.id == draft.calendarId }
        .takeIf { it >= 0 }
        ?: 0
    val startIndex = startOptions.indexOfFirst {
        it.hour == draft.startHour && it.minute == draft.startMinute
    }.takeIf { it >= 0 } ?: 0
    val durationIndex = durationOptions.indexOfFirst { it.minutes == draft.durationMinutes }
        .takeIf { it >= 0 }
        ?: durationOptions.indexOfFirst { it.minutes == 60 }.coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MetroText(
                text = stringResource(R.string.new_event_title),
                style = MetroTextStyle.SectionHeader,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(top = 24.dp, bottom = 8.dp),
            )
            MetroText(
                text = CalendarLogic.dateHeaderLabel(draft.epochDay),
                style = MetroTextStyle.Body,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 20.dp),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(bottom = MetroAppBarDefaults.BarHeight + 24.dp),
            ) {
                MetroText(
                    text = stringResource(R.string.event_field_subject),
                    style = MetroTextStyle.ListItemSubtitle,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                MetroTextBox(
                    value = draft.title,
                    onValueChange = { onDraftChange(draft.copy(title = it)) },
                    placeholder = stringResource(R.string.event_field_subject),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))
                MetroText(
                    text = stringResource(R.string.event_field_location),
                    style = MetroTextStyle.ListItemSubtitle,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                MetroTextBox(
                    value = draft.location,
                    onValueChange = { onDraftChange(draft.copy(location = it)) },
                    placeholder = stringResource(R.string.event_field_location),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                    ),
                )

                Spacer(modifier = Modifier.height(20.dp))
                MetroToggleSwitch(
                    checked = draft.allDay,
                    onCheckedChange = { onDraftChange(draft.copy(allDay = it)) },
                    label = stringResource(R.string.event_field_all_day),
                )

                if (!draft.allDay) {
                    Spacer(modifier = Modifier.height(16.dp))
                    MetroListPicker(
                        options = startOptions.map { it.label },
                        selectedOptionIndex = startIndex,
                        onSelectOption = { index ->
                            val option = startOptions[index]
                            onDraftChange(
                                draft.copy(startHour = option.hour, startMinute = option.minute),
                            )
                            openPicker = null
                        },
                        label = stringResource(R.string.event_field_starts),
                        expanded = openPicker == "start",
                        onExpandedChange = { expanded ->
                            openPicker = if (expanded) "start" else null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    MetroListPicker(
                        options = durationOptions.map { it.label },
                        selectedOptionIndex = durationIndex,
                        onSelectOption = { index ->
                            onDraftChange(draft.copy(durationMinutes = durationOptions[index].minutes))
                            openPicker = null
                        },
                        label = stringResource(R.string.event_field_duration),
                        expanded = openPicker == "duration",
                        onExpandedChange = { expanded ->
                            openPicker = if (expanded) "duration" else null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (calendars.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    MetroListPicker(
                        options = calendarLabels,
                        selectedOptionIndex = selectedCalendarIndex.coerceIn(0, calendars.lastIndex),
                        onSelectOption = { index ->
                            onDraftChange(draft.copy(calendarId = calendars[index].id))
                            openPicker = null
                        },
                        label = stringResource(R.string.event_field_calendar),
                        expanded = openPicker == "calendar",
                        onExpandedChange = { expanded ->
                            openPicker = if (expanded) "calendar" else null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    MetroText(
                        text = stringResource(R.string.event_no_calendar),
                        style = MetroTextStyle.Body,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Save,
                    label = stringResource(R.string.event_save),
                    onClick = onSave,
                    contentDescription = stringResource(R.string.event_save),
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Close,
                    label = stringResource(R.string.event_cancel),
                    onClick = onCancel,
                    contentDescription = stringResource(R.string.event_cancel),
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private data class StartTimeOption(val hour: Int, val minute: Int, val label: String)
private data class DurationOption(val minutes: Int, val label: String)

private fun startTimeLabels(): List<StartTimeOption> =
    (0 until 24).flatMap { hour ->
        listOf(0, 30).map { minute ->
            StartTimeOption(hour, minute, formatHourMinute(hour, minute))
        }
    }

private fun durationLabels(): List<DurationOption> = listOf(
    DurationOption(15, "15 minutes"),
    DurationOption(30, "30 minutes"),
    DurationOption(45, "45 minutes"),
    DurationOption(60, "1 hour"),
    DurationOption(90, "1.5 hours"),
    DurationOption(120, "2 hours"),
    DurationOption(180, "3 hours"),
)

private fun formatHourMinute(hour: Int, minute: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%d:%02d %s", displayHour, minute, period)
}

private fun calendarLabel(calendar: WritableCalendar): String {
    val account = calendar.accountName?.takeIf { it.isNotBlank() }
    return if (account != null && account != calendar.displayName) {
        "${calendar.displayName} ($account)"
    } else {
        calendar.displayName
    }
}
