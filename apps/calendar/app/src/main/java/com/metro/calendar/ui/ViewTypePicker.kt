package com.metro.calendar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarViewType
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions

@Composable
fun ViewTypePicker(
    visible: Boolean,
    currentType: CalendarViewType,
    onSelect: (CalendarViewType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = visible
    val showing = visibleState.currentState || visibleState.targetState

    Box(
        modifier = modifier.then(if (showing) Modifier.fillMaxSize() else Modifier),
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(MetroTransitions.AppBarSlideMs)),
            exit = fadeOut(animationSpec = tween(MetroTransitions.AppBarSlideMs)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visibleState = visibleState,
            enter = expandVertically(
                expandFrom = Alignment.Bottom,
                animationSpec = tween(MetroTransitions.AppBarSlideMs),
            ) + fadeIn(animationSpec = tween(MetroTransitions.AppBarSlideMs)),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Bottom,
                animationSpec = tween(MetroTransitions.AppBarSlideMs),
            ) + fadeOut(animationSpec = tween(MetroTransitions.AppBarSlideMs)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MetroAppBarDefaults.ChromeBackground)
                    .navigationBarsPadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* consume taps on panel */ },
                    )
                    .padding(vertical = 8.dp),
            ) {
                CalendarViewType.entries.forEach { type ->
                    ViewTypeRow(
                        label = viewTypeLabel(type),
                        selected = type == currentType,
                        onClick = { onSelect(type) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewTypeRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) MetroTheme.colors.accent else MetroTheme.colors.primaryText
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                fontFamily = MetroFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                color = color,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun viewTypeLabel(type: CalendarViewType): String = when (type) {
    CalendarViewType.Day -> stringResource(R.string.view_type_day)
    CalendarViewType.Week -> stringResource(R.string.view_type_week)
    CalendarViewType.Month -> stringResource(R.string.view_type_month)
    CalendarViewType.Year -> stringResource(R.string.view_type_year)
}
