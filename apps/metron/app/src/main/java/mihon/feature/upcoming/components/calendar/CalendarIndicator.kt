package mihon.feature.upcoming.components.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Square event tick for the upcoming month grid (WP8.1 calendar language).
 * Kept for call sites that still pass an index-based alpha; prefer inline ticks in [CalendarDay].
 */
@Composable
fun CalendarIndicator(
    index: Int,
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val alpha = ((index + 1) * 0.3f).coerceAtMost(1f)
    Box(
        modifier = modifier
            .background(color = color.copy(alpha = alpha), shape = RectangleShape)
            .size(width = size.div(7), height = 3.dp),
    )
}
