/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.app.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.metro.keyboard.ui.prefs.Preference
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.util.InputMethodUtils
import org.florisboard.lib.compose.stringRes

@Composable
fun HomeScreen() = FlorisScreen {
    title = "advanced"
    navigationIconVisible = false
    previewFieldVisible = true
    iconSpaceReserved = false

    val navController = LocalNavController.current
    val context = LocalContext.current

    content {
        val isFlorisBoardEnabled by InputMethodUtils.observeIsFlorisboardEnabled(foregroundOnly = true)
        val isFlorisBoardSelected by InputMethodUtils.observeIsFlorisboardSelected(foregroundOnly = true)
        if (!isFlorisBoardEnabled) {
            MetroSetupBanner(
                title = "enable keyboard",
                body = stringRes(R.string.settings__home__ime_not_enabled),
                action = "enable",
                onAction = { InputMethodUtils.showImeEnablerActivity(context) },
            )
        } else if (!isFlorisBoardSelected) {
            MetroSetupBanner(
                title = "select keyboard",
                body = stringRes(R.string.settings__home__ime_not_selected),
                action = "select",
                onAction = { InputMethodUtils.showImePicker(context) },
            )
        }

        Preference(
            title = stringRes(R.string.settings__localization__title),
            summary = "writing languages and layouts",
            onClick = { navController.navigate(Routes.Settings.Localization) },
        )
        Preference(
            title = stringRes(R.string.settings__theme__title),
            summary = "day and night keyboard look",
            onClick = { navController.navigate(Routes.Settings.Theme) },
        )
        Preference(
            title = stringRes(R.string.settings__keyboard__title),
            summary = "keys, spacing, and long-press",
            onClick = { navController.navigate(Routes.Settings.Keyboard) },
        )
        Preference(
            title = stringRes(R.string.settings__smartbar__title),
            summary = "suggestion bar layout",
            onClick = { navController.navigate(Routes.Settings.Smartbar) },
        )
        Preference(
            title = stringRes(R.string.settings__typing__title),
            summary = "suggestions, correction, and spelling",
            onClick = { navController.navigate(Routes.Settings.Typing) },
        )
        Preference(
            title = stringRes(R.string.settings__gestures__title),
            summary = "swipes and glide typing",
            onClick = { navController.navigate(Routes.Settings.Gestures) },
        )
        Preference(
            title = stringRes(R.string.settings__clipboard__title),
            summary = "clipboard history and suggestions",
            onClick = { navController.navigate(Routes.Settings.Clipboard) },
        )
        Preference(
            title = stringRes(R.string.settings__media__title),
            summary = "emoji history and suggestions",
            onClick = { navController.navigate(Routes.Settings.Media) },
        )
        Preference(
            title = stringRes(R.string.ext__home__title),
            summary = "themes, layouts, and language packs",
            onClick = { navController.navigate(Routes.Ext.Home) },
        )
        Preference(
            title = stringRes(R.string.settings__other__title),
            summary = "backup, restore, and developer tools",
            onClick = { navController.navigate(Routes.Settings.Other) },
        )
        Preference(
            title = stringRes(R.string.about__title),
            summary = "version and licenses",
            onClick = { navController.navigate(Routes.Settings.About) },
        )
    }
}

@Composable
private fun MetroSetupBanner(
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
