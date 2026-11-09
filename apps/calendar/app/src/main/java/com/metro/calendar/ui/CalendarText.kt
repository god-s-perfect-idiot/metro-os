package com.metro.calendar.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/** Single-line calendar chrome — clips at the screen edge, never wraps. */
@Composable
internal fun CalendarLineText(
    text: String,
    style: MetroTextStyle,
    modifier: Modifier = Modifier,
    color: Color = MetroTheme.colors.primaryText,
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
) {
    val base = style.toTextStyle(MetroTheme.fontFamily)
    BasicText(
        text = text,
        style = base.copy(
            color = color,
            fontSize = if (fontSize != TextUnit.Unspecified) fontSize else base.fontSize,
            lineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight else base.lineHeight,
        ),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier.fillMaxWidth(),
    )
}
