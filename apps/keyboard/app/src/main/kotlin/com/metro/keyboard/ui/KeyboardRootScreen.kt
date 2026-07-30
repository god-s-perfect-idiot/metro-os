package com.metro.keyboard.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import dev.patrickgold.florisboard.lib.util.InputMethodUtils

private val SettingsRowHeight = 76.dp

@Composable
fun KeyboardRootScreen(
    onOpenLanguage: (String) -> Unit,
    onOpenAdvanced: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imeEnabled by InputMethodUtils.observeIsFlorisboardEnabled(foregroundOnly = true)
    val imeSelected by InputMethodUtils.observeIsFlorisboardSelected(foregroundOnly = true)

    LazyColumn(modifier = modifier) {
        item {
            MetroSettingsHeader(pageTitle = "keyboard")
        }

        if (!imeEnabled) {
            item {
                SetupBanner(
                    title = "Enable Keyboard",
                    body = "Turn on Metro Keyboard in Android input settings to type with the WP8.1 SIP.",
                    action = "enable",
                    onAction = { InputMethodUtils.showImeEnablerActivity(context) },
                )
            }
        } else if (!imeSelected) {
            item {
                SetupBanner(
                    title = "Select Keyboard",
                    body = "Choose Metro Keyboard as your current input method.",
                    action = "select",
                    onAction = { InputMethodUtils.showImePicker(context) },
                )
            }
        }

        item {
            MetroText(
                text = "writing languages",
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent,
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    end = MetroDimens.ScreenHorizontalMargin,
                    top = 8.dp,
                    bottom = 4.dp,
                ),
            )
        }
        item {
            MetroListItem(
                title = "English (United States)",
                subtitle = if (imeEnabled && imeSelected) "on" else "not selected",
                modifier = Modifier.height(SettingsRowHeight),
                onClick = { onOpenLanguage("English (United States)") },
            )
        }
        item {
            MetroListItem(
                title = "add keyboards",
                subtitle = "install more writing languages",
                modifier = Modifier.height(SettingsRowHeight),
                onClick = { onOpenLanguage("add keyboards") },
            )
        }
        item {
            MetroListItem(
                title = "advanced",
                subtitle = "comma key and more",
                modifier = Modifier.height(SettingsRowHeight),
                onClick = onOpenAdvanced,
            )
        }
    }
}

@Composable
private fun SetupBanner(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 12.dp,
            ),
    ) {
        MetroText(text = title, style = MetroTextStyle.ListItemTitle)
        MetroText(
            text = body,
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        MetroBorderButton(text = action, onClick = onAction)
    }
}
