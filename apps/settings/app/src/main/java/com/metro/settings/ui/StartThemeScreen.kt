package com.metro.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.metro.settings.R
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun StartThemeScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    val accentPhrase = stringResource(R.string.settings_start_theme_intro_accent)
    val introPrefix = stringResource(R.string.settings_start_theme_intro_prefix)
    val introSuffix = stringResource(R.string.settings_start_theme_intro_suffix)
    val accent = MetroTheme.colors.accent
    val intro = remember(introPrefix, accentPhrase, introSuffix, accent) {
        buildAnnotatedString {
            append(introPrefix)
            withStyle(SpanStyle(color = accent)) {
                append(accentPhrase)
            }
            append(introSuffix)
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        MetroSettingsHeader(pageTitle = stringResource(R.string.settings_start_theme))

        MetroText(
            text = intro,
            style = MetroTextStyle.Body,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                bottom = 28.dp,
            ),
        )

        MetroText(
            text = stringResource(R.string.settings_accent_label),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                bottom = 8.dp,
            ),
        )

        Row(
            modifier = Modifier
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .border(1.dp, MetroTheme.colors.primaryText)
                .clickable { state.open(SettingsRoute.AccentPicker) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(state.accentColor),
            )
            Spacer(modifier = Modifier.width(12.dp))
            MetroText(
                text = state.accentDisplayName,
                style = MetroTextStyle.ListItemTitle,
            )
        }
    }
}
