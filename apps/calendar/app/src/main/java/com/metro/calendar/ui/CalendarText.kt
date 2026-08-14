package com.metro.calendar.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/** Single-line calendar chrome — clips at the screen edge, never wraps. */
@Composable
internal fun CalendarLineText(
    text: String,
    style: MetroTextStyle,
    modifier: Modifier = Modifier,
    color: Color = MetroTheme.colors.primaryText,
) {
    MetroText(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.fillMaxWidth(),
    )
}
