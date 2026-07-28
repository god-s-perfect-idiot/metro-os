package com.metro.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
internal fun SettingsDetailScaffold(
    pageTitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        MetroSettingsHeader(pageTitle = pageTitle)
        content()
    }
}

/**
 * Secondary field label under a settings page title (WP: Accent colour, Brightness, Text size).
 */
@Composable
internal fun SettingsFieldLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    MetroText(
        text = text,
        style = MetroTextStyle.ListItemSubtitle,
        color = MetroTheme.colors.secondaryText,
        modifier = modifier.padding(
            start = MetroDimens.ScreenHorizontalMargin,
            end = MetroDimens.ScreenHorizontalMargin,
            bottom = 8.dp,
        ),
    )
}

@Composable
internal fun SettingsBodyText(
    text: String,
    modifier: Modifier = Modifier,
) {
    MetroText(
        text = text,
        style = MetroTextStyle.Body,
        modifier = modifier.padding(
            start = MetroDimens.ScreenHorizontalMargin,
            end = MetroDimens.ScreenHorizontalMargin,
            bottom = 20.dp,
        ),
    )
}

@Composable
internal fun SettingsStatusRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroText(
            text = label,
            style = MetroTextStyle.Body,
            modifier = Modifier.weight(1f),
        )
        MetroText(
            text = value,
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
        )
    }
}

@Composable
internal fun SettingsHelpText(
    text: String,
    modifier: Modifier = Modifier,
) {
    MetroText(
        text = text,
        style = MetroTextStyle.Body,
        color = MetroTheme.colors.primaryText,
        modifier = modifier.padding(
            horizontal = MetroDimens.ScreenHorizontalMargin,
            vertical = 12.dp,
        ),
    )
}

@Composable
internal fun SettingsSpacer(height: Int = 16) {
    Spacer(modifier = Modifier.height(height.dp))
}
