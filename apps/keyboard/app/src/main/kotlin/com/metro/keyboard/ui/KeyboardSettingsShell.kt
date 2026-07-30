package com.metro.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.navigation.compose.rememberNavController
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.setup.NotificationPermissionState
import dev.patrickgold.florisboard.lib.compose.LocalPreviewFieldController
import dev.patrickgold.florisboard.lib.compose.PreviewKeyboardField
import dev.patrickgold.florisboard.lib.compose.rememberPreviewFieldController
import dev.patrickgold.jetpref.datastore.ui.ProvideDefaultDialogPrefStrings
import org.florisboard.lib.compose.stringRes

enum class KeyboardSettingsRoute {
    Root,
    Language,
    AddKeyboards,
    FlorisSettings,
}

@Composable
fun KeyboardSettingsShell(
    showWelcomeIntro: Boolean = false,
    showFinishAction: Boolean = false,
    notificationPermissionState: NotificationPermissionState? = null,
    onRequestNotification: (() -> Unit)? = null,
    onSetupComplete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var route by remember { mutableStateOf(KeyboardSettingsRoute.Root) }
    var languageLabel by remember { mutableStateOf("english (united states)") }

    when (route) {
        KeyboardSettingsRoute.FlorisSettings -> {
            FlorisSettingsHost(
                modifier = modifier,
                onExitToMetroRoot = { route = KeyboardSettingsRoute.Root },
            )
        }
        else -> {
            BackHandler(enabled = route != KeyboardSettingsRoute.Root) {
                route = KeyboardSettingsRoute.Root
            }

            val contentModifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .metroNavBarPadding()
                .background(MetroTheme.colors.background)

            when (route) {
                KeyboardSettingsRoute.Root -> KeyboardRootScreen(
                    modifier = contentModifier,
                    showWelcomeIntro = showWelcomeIntro,
                    showFinishAction = showFinishAction,
                    notificationPermissionState = notificationPermissionState,
                    onRequestNotification = onRequestNotification,
                    onSetupComplete = onSetupComplete,
                    onOpenLanguage = { label ->
                        languageLabel = label
                        route = KeyboardSettingsRoute.Language
                    },
                    onOpenAddKeyboards = { route = KeyboardSettingsRoute.AddKeyboards },
                    onOpenAdvanced = { route = KeyboardSettingsRoute.FlorisSettings },
                )
                KeyboardSettingsRoute.Language -> KeyboardLanguageScreen(
                    languageLabel = languageLabel,
                    modifier = contentModifier,
                )
                KeyboardSettingsRoute.AddKeyboards -> KeyboardAddKeyboardsScreen(
                    modifier = contentModifier,
                )
                KeyboardSettingsRoute.FlorisSettings -> Unit
            }
        }
    }
}

/**
 * Hosts the full FlorisBoard settings NavHost under Metro chrome.
 * Back from the settings home returns to the WP8.1 keyboard front page.
 */
@Composable
fun FlorisSettingsHost(
    onExitToMetroRoot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val previewFieldController = rememberPreviewFieldController()

    BackHandler {
        if (!navController.popBackStack()) {
            onExitToMetroRoot()
        }
    }

    CompositionLocalProvider(
        LocalNavController provides navController,
        LocalPreviewFieldController provides previewFieldController,
    ) {
        ProvideDefaultDialogPrefStrings(
            confirmLabel = stringRes(R.string.action__ok),
            dismissLabel = stringRes(R.string.action__cancel),
            neutralLabel = stringRes(R.string.action__default),
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .imePadding(),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Routes.AppNavHost(
                        modifier = Modifier.fillMaxSize(),
                        navController = navController,
                        startDestination = Routes.Settings.Home::class,
                    )
                }
                PreviewKeyboardField(previewFieldController)
            }
        }
    }
}
