package mihon.feature.upcoming.components.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private const val MAX_EVENTS = 3

@Composable
fun CalendarDay(
    date: LocalDate,
    events: Int,
    onDayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val isToday = today == date
    val isPast = date < today
    val textColor = when {
        isPast -> MetroTheme.colors.secondaryText.copy(alpha = 0.45f)
        else -> MetroTheme.colors.primaryText
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .border(0.5.dp, MetroTheme.colors.secondaryText.copy(alpha = 0.3f), RectangleShape)
            .then(
                if (isToday) {
                    Modifier.border(1.dp, MetroTheme.colors.accent, RectangleShape)
                } else {
                    Modifier
                },
            )
            .metroClickable(onClick = onDayClick)
            .padding(4.dp),
    ) {
        MetroText(
            text = date.day.toString(),
            style = MetroTextStyle.ListItemSubtitle,
            color = textColor,
            modifier = Modifier.align(Alignment.TopStart),
        )
        if (isToday) {
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
            val size = events.coerceAtMost(MAX_EVENTS)
            repeat(size) {
                Box(
                    modifier = Modifier
                        .size(width = 8.dp, height = 3.dp)
                        .background(MetroTheme.colors.accent),
                )
            }
        }
    }
}
