package com.metro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * WP8.1 rectangular outlined text button — white (dark) / black (light) square border,
 * theme text, theme background. See [METRO-UX-LANGUAGE.md] §6.3.
 */
@Composable
fun MetroBorderButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val foreground = MetroTheme.colors.primaryText
    val borderColor = if (enabled) foreground else foreground.copy(alpha = 0.4f)
    val textColor = if (enabled) foreground else foreground.copy(alpha = 0.4f)
    val background = when {
        !enabled -> MetroTheme.colors.background
        pressed -> foreground.copy(alpha = 0.2f)
        else -> MetroTheme.colors.background
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .background(background, RectangleShape)
            .border(width = 3.dp, color = borderColor, shape = RectangleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                fontFamily = MetroFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = textColor,
                textAlign = TextAlign.Center,
            ),
            maxLines = 2,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MetroBorderButtonDarkPreview() {
    MetroTheme(darkTheme = true) {
        MetroBorderButton(text = "allow access", onClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MetroBorderButtonLightPreview() {
    MetroTheme(darkTheme = false) {
        MetroBorderButton(text = "allow access", onClick = {})
    }
}
