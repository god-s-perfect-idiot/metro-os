package mihon.feature.upcoming.components.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.cheonjaeung.compose.grid.SimpleGridCells
import com.cheonjaeung.compose.grid.VerticalGrid
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth
import kotlinx.datetime.toJavaDayOfWeek
import mihon.core.designsystem.utils.isExpandedWidthWindow
import mihon.core.designsystem.utils.isMediumWidthWindow
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private const val DAYS_OF_WEEK = 7

@Composable
fun Calendar(
    selectedYearMonth: YearMonth,
    events: Map<LocalDate, Int>,
    setSelectedYearMonth: (YearMonth) -> Unit,
    onClickDay: (day: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CalenderHeader(
            yearMonth = selectedYearMonth,
            onPreviousClick = { setSelectedYearMonth(selectedYearMonth.minusMonth()) },
            onNextClick = { setSelectedYearMonth(selectedYearMonth.plusMonth()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
        CalendarGrid(
            selectedYearMonth = selectedYearMonth,
            events = events,
            onClickDay = onClickDay,
        )
    }
}

@Composable
private fun CalendarGrid(
    selectedYearMonth: YearMonth,
    events: Map<LocalDate, Int>,
    onClickDay: (day: LocalDate) -> Unit,
) {
    val localeFirstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek.value
    val weekDays = remember {
        (0 until DAYS_OF_WEEK)
            .map { DayOfWeek.of((localeFirstDayOfWeek - 1 + it) % DAYS_OF_WEEK + 1) }
    }

    val emptyFieldCount = weekDays.indexOf(selectedYearMonth.firstDay.dayOfWeek.toJavaDayOfWeek())
    val daysInMonth = selectedYearMonth.numberOfDays

    VerticalGrid(
        columns = SimpleGridCells.Fixed(DAYS_OF_WEEK),
        modifier = if (isMediumWidthWindow() && !isExpandedWidthWindow()) {
            Modifier.widthIn(max = 360.dp)
        } else {
            Modifier
        },
    ) {
        weekDays.fastForEach { item ->
            MetroText(
                text = item.getDisplayName(
                    TextStyle.NARROW,
                    Locale.getDefault(),
                ),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        repeat(emptyFieldCount) { Box { } }
        repeat(daysInMonth) { dayIndex ->
            val localDate = LocalDate(selectedYearMonth.year, selectedYearMonth.month, dayIndex + 1)
            CalendarDay(
                date = localDate,
                onDayClick = { onClickDay(localDate) },
                events = events[localDate] ?: 0,
            )
        }
    }
}
