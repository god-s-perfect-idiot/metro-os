package com.metro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Accent square tile for alphabet jump list anchors and contact section headers.
 */
@Composable
fun MetroLetterTile(
    letter: Char,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    onClick: (() -> Unit)? = null,
) {
    val display = if (letter == '#') "#" else letter.lowercaseChar().toString()
    Box(
        modifier = modifier
            .size(size)
            .background(MetroTheme.colors.accent)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        MetroText(
            text = display,
            style = MetroTextStyle.ListItemTitle,
            color = Color.White,
        )
    }
}
