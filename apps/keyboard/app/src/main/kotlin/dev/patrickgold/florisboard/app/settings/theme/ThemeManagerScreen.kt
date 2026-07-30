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

package dev.patrickgold.florisboard.app.settings.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionComponent
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import dev.patrickgold.florisboard.themeManager
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes

enum class ThemeManagerScreenAction(val id: String) {
    SELECT_DAY("select-day"),
    SELECT_NIGHT("select-night");
}

@Composable
fun ThemeManagerScreen(action: ThemeManagerScreenAction?) = FlorisScreen {
    title = stringRes(when (action) {
        ThemeManagerScreenAction.SELECT_DAY -> R.string.settings__theme_manager__title_day
        ThemeManagerScreenAction.SELECT_NIGHT -> R.string.settings__theme_manager__title_night
        else -> error("Theme manager screen action must not be null")
    })
    previewFieldVisible = true

    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val extensionManager by context.extensionManager()
    val themeManager by context.themeManager()
    val scope = rememberCoroutineScope()

    val indexedThemeExtensions by extensionManager.themes.collectAsState()
    val extGroupedThemes = remember(indexedThemeExtensions) {
        buildMap<String, List<ThemeExtensionComponent>> {
            for (ext in indexedThemeExtensions) {
                put(ext.meta.id, ext.themes)
            }
        }.mapValues { (_, configs) -> configs.sortedBy { it.label } }
    }

    fun getThemeIdPref() = when (action) {
        ThemeManagerScreenAction.SELECT_DAY -> prefs.theme.dayThemeId
        ThemeManagerScreenAction.SELECT_NIGHT -> prefs.theme.nightThemeId
    }

    fun setTheme(extId: String, componentId: String) {
        val extComponentName = ExtensionComponentName(extId, componentId)
        when (action) {
            ThemeManagerScreenAction.SELECT_DAY,
            ThemeManagerScreenAction.SELECT_NIGHT -> scope.launch {
                getThemeIdPref().set(extComponentName)
            }
        }
    }

    val activeThemeId by when (action) {
        ThemeManagerScreenAction.SELECT_DAY,
        ThemeManagerScreenAction.SELECT_NIGHT
            -> getThemeIdPref().collectAsState()
    }

    content {
        DisposableEffect(activeThemeId) {
            themeManager.previewThemeId.value = activeThemeId
            onDispose {
                themeManager.previewThemeId.value = null
            }
        }
        for ((extensionId, configs) in extGroupedThemes) key(extensionId) {
            val ext = extensionManager.getExtensionById(extensionId)!!
            MetroText(
                text = ext.meta.title.lowercase(),
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent,
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    end = MetroDimens.ScreenHorizontalMargin,
                    top = 12.dp,
                    bottom = 2.dp,
                ),
            )
            MetroText(
                text = extensionId,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    end = MetroDimens.ScreenHorizontalMargin,
                    bottom = 4.dp,
                ),
            )
            for (config in configs) key(extensionId, config.id) {
                val selected = activeThemeId.extensionId == extensionId &&
                    activeThemeId.componentId == config.id
                MetroListItem(
                    title = config.label.lowercase(),
                    subtitle = if (config.isNightTheme) "night" else "day",
                    trailing = {
                        MetroText(
                            text = if (selected) "●" else "○",
                            style = MetroTextStyle.ListItemTitle,
                            color = if (selected) MetroTheme.colors.accent else MetroTheme.colors.secondaryText,
                        )
                    },
                    verticalPadding = 8.dp,
                    oneLineMinHeight = 56.dp,
                    twoLineMinHeight = 68.dp,
                    onClick = { setTheme(extensionId, config.id) },
                )
            }
        }
    }
}
