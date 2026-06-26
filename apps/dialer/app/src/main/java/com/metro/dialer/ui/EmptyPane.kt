package com.metro.dialer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.metro.ui.MetroEmptyState

@Composable
fun EmptyPane(
    message: String,
    modifier: Modifier = Modifier,
) {
    MetroEmptyState(message = message, modifier = modifier)
}
