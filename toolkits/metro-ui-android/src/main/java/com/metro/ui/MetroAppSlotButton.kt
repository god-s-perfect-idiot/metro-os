package com.metro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val SlotBorderWidth = 2.dp

object MetroAppSlotDefaults {
    val Size: Dp = 48.dp
    val GlyphScale = 0.55f
    val AddIconScale = 0.70f
}

private val DefaultSlotSize = MetroAppSlotDefaults.Size
private val GlyphScale = MetroAppSlotDefaults.GlyphScale
/** Slightly larger than the glyph scale so the empty plus reads clearly in the smaller slot. */
private val AddIconScale = MetroAppSlotDefaults.AddIconScale

/**
 * WP8.1 lock-screen quick-status slot — square bordered button showing an app glyph or a plus
 * when empty (METRO-UX-LANGUAGE §6.20).
 *
 * Pass [iconResId] for suite / tile glyphs, or [iconPainter] for a resolved launcher icon.
 * The plus is shown only when both are null.
 */
@Composable
fun MetroAppSlotButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = DefaultSlotSize,
    iconResId: Int? = null,
    iconPainter: Painter? = null,
    iconTint: Color = MetroTheme.colors.primaryText,
    contentDescription: String? = null,
    enabled: Boolean = true,
) {
    val borderColor = MetroTheme.colors.primaryText.copy(
        alpha = if (enabled) 1f else 0.4f,
    )
    Box(
        modifier = modifier
            .size(size)
            .border(SlotBorderWidth, borderColor, RectangleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            iconResId != null -> {
                Image(
                    painter = painterResource(iconResId),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(iconTint),
                    modifier = Modifier.size(size * GlyphScale),
                )
            }
            iconPainter != null -> {
                Image(
                    painter = iconPainter,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(iconTint),
                    modifier = Modifier.size(size * GlyphScale),
                )
            }
            else -> {
                MetroSystemIcon(
                    type = MetroSystemIconType.Add,
                    iconSize = size * AddIconScale,
                    color = iconTint.copy(alpha = iconTint.alpha * if (enabled) 1f else 0.4f),
                    showCircle = false,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MetroAppSlotButtonEmptyPreview() {
    MetroTheme(darkTheme = true) {
        MetroAppSlotButton(onClick = {}, iconResId = null)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MetroAppSlotButtonFilledPreview() {
    MetroTheme(darkTheme = true) {
        MetroAppSlotButton(onClick = {}, iconResId = MetroAppGlyphs.Phone)
    }
}
