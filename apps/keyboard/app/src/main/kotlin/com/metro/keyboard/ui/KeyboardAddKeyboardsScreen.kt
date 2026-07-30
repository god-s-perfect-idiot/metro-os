package com.metro.keyboard.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun KeyboardAddKeyboardsScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        MetroSettingsHeader(
            pageTitle = "add keyboards",
            appTitle = "keyboard",
        )
        MetroText(
            text = "This build currently includes english (united states) only.",
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 8.dp,
            ),
        )
        MetroText(
            text = "Additional writing languages will appear here after the Metro language picker is finished.",
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 8.dp,
            ),
        )
    }
}
