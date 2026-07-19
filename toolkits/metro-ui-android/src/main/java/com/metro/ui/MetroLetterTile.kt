package com.metro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Accent square tile for alphabet jump list anchors and contact section headers.
 *
 * When [enabled] is false (no items for that letter), the tile uses a dark inactive
 * fill matching WP8.1 jump list gray tiles and ignores clicks.
 */
@Composable
fun MetroLetterTile(
    letter: Char,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val display = if (letter == '#') "#" else letter.lowercaseChar().toString()
    val background = if (enabled) {
        MetroTheme.colors.accent
    } else {
        MetroColors.JumpListInactive
    }
    Box(
        modifier = modifier
            .size(size)
            .background(background)
            .semantics {
                role = Role.Button
                contentDescription = "jump to $display"
            }
            .then(
                if (onClick != null) {
                    // Always consume the tap so inactive jump tiles do not dismiss a parent scrim.
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (enabled) onClick() },
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        MetroText(
            text = display,
            style = MetroTextStyle.ListItemTitle,
            color = Color.White,
        )
    }
}
