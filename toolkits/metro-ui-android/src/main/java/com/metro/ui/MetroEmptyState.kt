package com.metro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * WP8.1 standard empty / support text shown when a list or page has no content yet.
 *
 * Standard across all metro-os apps: large ([MetroTextStyle.ListItemTitle], 24sp),
 * left- and top-aligned, secondary foreground, 24dp page margins. Matches the WP8.1
 * Phone app's "no recent calls" treatment. Do not center or shrink empty-state text.
 */
@Composable
fun MetroEmptyState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 24.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        MetroText(
            text = message,
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.secondaryText,
        )
    }
}
