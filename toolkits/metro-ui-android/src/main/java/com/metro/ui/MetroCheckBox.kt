package com.metro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * WP8.1 checkbox — **20×20dp square**, 2dp border (METRO-UX-LANGUAGE §6.10).
 *
 * Checked: accent fill + white checkmark. Unchecked: primary-text outline, transparent fill.
 * Prefer [MetroToggleSwitch] for on/off settings; use this for multi-select lists.
 *
 * When [onCheckedChange] is null, the control is display-only (parent row owns the tap).
 */
@Composable
fun MetroCheckBox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = MetroCheckBoxDefaults.Size,
) {
    val border = MetroTheme.colors.primaryText.copy(alpha = if (enabled) 1f else 0.4f)
    val accent = MetroTheme.colors.accent.copy(alpha = if (enabled) 1f else 0.4f)
    val checkColor = MetroColors.DarkPrimaryText
    val interactive = onCheckedChange != null && enabled
    Box(
        modifier = modifier
            .then(
                if (interactive) {
                    Modifier
                        .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                        .clickable { onCheckedChange!!(!checked) }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = MetroCheckBoxDefaults.BorderWidth.toPx()
            if (checked) {
                drawRect(color = accent)
                drawMetroCheckGlyph(checkColor, glyphScale = MetroCheckBoxDefaults.CheckGlyphScale)
            } else {
                drawRect(
                    color = border,
                    topLeft = Offset(strokePx / 2f, strokePx / 2f),
                    size = Size(this.size.width - strokePx, this.size.height - strokePx),
                    style = Stroke(width = strokePx),
                )
            }
        }
    }
}

object MetroCheckBoxDefaults {
    val Size: Dp = 20.dp
    val BorderWidth: Dp = 2.dp
    /** Fraction of the box filled by the shared WP8.1 check path viewBox. */
    const val CheckGlyphScale: Float = 0.88f
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MetroCheckBoxDarkPreview() {
    MetroTheme(darkTheme = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetroCheckBox(checked = false, onCheckedChange = {})
            MetroCheckBox(checked = true, onCheckedChange = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MetroCheckBoxLightPreview() {
    MetroTheme(darkTheme = false) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetroCheckBox(checked = false, onCheckedChange = {})
            MetroCheckBox(checked = true, onCheckedChange = {})
        }
    }
}
