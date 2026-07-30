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
import dev.patrickgold.florisboard.app.setup.NotificationPermissionState
import dev.patrickgold.florisboard.lib.util.InputMethodUtils
import org.florisboard.lib.android.AndroidVersion

private val SettingsRowHeight = 76.dp

@Composable
fun KeyboardRootScreen(
    showWelcomeIntro: Boolean = false,
    showFinishAction: Boolean = false,
    notificationPermissionState: NotificationPermissionState? = null,
    onRequestNotification: (() -> Unit)? = null,
    onSetupComplete: (() -> Unit)? = null,
    onOpenLanguage: (String) -> Unit,
    onOpenAddKeyboards: () -> Unit,
    onOpenAdvanced: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imeEnabled by InputMethodUtils.observeIsFlorisboardEnabled(foregroundOnly = true)
    val imeSelected by InputMethodUtils.observeIsFlorisboardSelected(foregroundOnly = true)

    LazyColumn(modifier = modifier) {
        item {
            MetroSettingsHeader(
                pageTitle = "keyboard",
                appTitle = "keyboard",
            )
        }

        if (showWelcomeIntro) {
            item {
                MetroText(
                    text = "Choose writing languages and typing options for the Windows Phone 8.1 keyboard on this phone.",
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(
                        start = MetroDimens.ScreenHorizontalMargin,
                        end = MetroDimens.ScreenHorizontalMargin,
                        bottom = 12.dp,
                    ),
                )
            }
        }

        if (!imeEnabled) {
            item {
                SetupBanner(
                    title = "enable keyboard",
                    body = "Turn on Keyboard in Android input settings so it can appear as an input method.",
                    action = "enable",
                    onAction = { InputMethodUtils.showImeEnablerActivity(context) },
                )
            }
        }

        if (imeEnabled && !imeSelected) {
            item {
                SetupBanner(
                    title = "select keyboard",
                    body = "Choose Keyboard as your current input method to start typing with the WP8.1 SIP.",
                    action = "select",
                    onAction = { InputMethodUtils.showImePicker(context) },
                )
            }
        }

        val needsNotificationPermission =
            AndroidVersion.ATLEAST_API33_T && notificationPermissionState == NotificationPermissionState.NOT_SET
        if (imeEnabled && imeSelected && needsNotificationPermission && onRequestNotification != null) {
            item {
                SetupBanner(
                    title = "allow notifications",
                    body = "Allow notifications so crash reports can open if the keyboard fails.",
                    action = "allow",
                    onAction = onRequestNotification,
                )
            }
        }

        val canFinishSetup = imeEnabled &&
            imeSelected &&
            (!AndroidVersion.ATLEAST_API33_T || notificationPermissionState != NotificationPermissionState.NOT_SET)
        if (showFinishAction && canFinishSetup && onSetupComplete != null) {
            item {
                SetupBanner(
                    title = "ready",
                    body = "Keyboard is enabled and selected. Open a text field to use the WP8.1 SIP.",
                    action = "continue",
                    onAction = onSetupComplete,
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
                title = "english (united states)",
                subtitle = if (imeEnabled && imeSelected) "suggest text and typing options" else "not selected",
                modifier = Modifier.height(SettingsRowHeight),
                onClick = { onOpenLanguage("english (united states)") },
            )
        }
        item {
            MetroListItem(
                title = "add keyboards",
                subtitle = "writing languages available in this build",
                modifier = Modifier.height(SettingsRowHeight),
                onClick = onOpenAddKeyboards,
            )
        }
        item {
            MetroListItem(
                title = "advanced",
                subtitle = "languages, typing, theme, gestures, and more",
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
