package com.metro.calendar.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarViewType
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun ViewTypePicker(
    currentType: CalendarViewType,
    onSelect: (CalendarViewType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
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

@Composable
private fun ViewTypeRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
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
        MetroText(
            text = label,
            style = MetroTextStyle.ListItemTitle,
            color = if (selected) MetroTheme.colors.accent else MetroTheme.colors.primaryText,
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
