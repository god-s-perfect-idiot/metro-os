package com.metro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * WP8.1 toggle — pill track 38×20dp, accent when on (METRO-UX-LANGUAGE §6.9).
 */
@Composable
fun MetroToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
) {
    val accent = MetroTheme.colors.accent
    val foreground = MetroTheme.colors.primaryText
    val trackShape = RoundedCornerShape(percent = 50)
    val trackModifier = Modifier
        .size(width = 38.dp, height = 20.dp)
        .then(
            if (checked) {
                Modifier.background(accent, trackShape)
            } else {
                Modifier.border(2.dp, foreground.copy(alpha = 0.2f), trackShape)
            },
        )

    Row(
        modifier = modifier
            .height(44.dp)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (label != null) {
            Box(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                MetroText(
                    text = label,
                    style = MetroTextStyle.Body,
                    color = if (enabled) foreground else foreground.copy(alpha = 0.4f),
                )
            }
        }
        Box(
            modifier = trackModifier,
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .size(16.dp)
                    .background(
                        color = if (checked) Color.White else foreground,
                        shape = RoundedCornerShape(percent = 50),
                    ),
            )
        }
    }
}
