package mihon.feature.upcoming.components.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toJavaYearMonth
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Clock

@Composable
fun CalenderHeader(
    yearMonth: YearMonth,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = yearMonth,
            transitionSpec = { getAnimation() },
            label = "Change Month",
        ) { monthYear ->
            MetroText(
                text = getTitleText(monthYear).lowercase(),
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.primaryText,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .metroClickable(onClick = onPreviousClick),
                contentAlignment = Alignment.Center,
            ) {
                MetroSystemIcon(
                    type = MetroSystemIconType.ChevronLeft,
                    iconSize = 28.dp,
                    color = MetroTheme.colors.primaryText,
                    showCircle = false,
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .metroClickable(onClick = onNextClick),
                contentAlignment = Alignment.Center,
            ) {
                MetroSystemIcon(
                    type = MetroSystemIconType.ChevronRight,
                    iconSize = 28.dp,
                    color = MetroTheme.colors.primaryText,
                    showCircle = false,
                )
            }
        }
    }
}

private const val MONTH_YEAR_CHANGE_ANIMATION_DURATION = 200

private fun AnimatedContentTransitionScope<YearMonth>.getAnimation(): ContentTransform {
    val movingForward = targetState > initialState

    val enterTransition = slideInVertically(
        animationSpec = tween(durationMillis = MONTH_YEAR_CHANGE_ANIMATION_DURATION),
    ) { height -> if (movingForward) height else -height } + fadeIn(
        animationSpec = tween(durationMillis = MONTH_YEAR_CHANGE_ANIMATION_DURATION),
    )
    val exitTransition = slideOutVertically(
        animationSpec = tween(durationMillis = MONTH_YEAR_CHANGE_ANIMATION_DURATION),
    ) { height -> if (movingForward) -height else height } + fadeOut(
        animationSpec = tween(durationMillis = MONTH_YEAR_CHANGE_ANIMATION_DURATION),
    )
    return (enterTransition togetherWith exitTransition)
        .using(SizeTransform(clip = false))
}

@Composable
@ReadOnlyComposable
private fun getTitleText(monthYear: YearMonth): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    return formatter.format(monthYear.toJavaYearMonth())
}

@Preview
@Composable
private fun CalenderHeaderPreview() {
    CalenderHeader(
        yearMonth = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.yearMonth,
        onNextClick = {},
        onPreviousClick = {},
    )
}
