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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * WP8.1 rectangular outlined text button — white (dark) / black (light) square border,
 * theme text, transparent rest fill. See [METRO-UX-LANGUAGE.md] §6.3.
 *
 * Visual chrome sizes to the label (WP Button padding 10×3–5); the 44dp min height is the
 * touch target around the border, not extra inset inside it.
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
        !enabled -> Color.Transparent
        pressed -> foreground.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .background(background, RectangleShape)
                .border(width = 2.dp, color = borderColor, shape = RectangleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
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
