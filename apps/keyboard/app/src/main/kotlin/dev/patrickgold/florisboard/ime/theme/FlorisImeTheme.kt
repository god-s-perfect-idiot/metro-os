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

package dev.patrickgold.florisboard.ime.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroPreferences
import com.metro.system.MetroThemeMode
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.window.LocalWindowController
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.themeManager
import dev.patrickgold.jetpref.datastore.model.collectAsState
import org.florisboard.lib.snygg.ui.ProvideSnyggTheme
import org.florisboard.lib.snygg.ui.rememberSnyggTheme

@Composable
fun FlorisImeTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val windowController = LocalWindowController.current

    val keyboardManager by context.keyboardManager()
    val themeManager by context.themeManager()

    val prefs by FlorisPreferenceStore
    val florisAccent by prefs.theme.accentColor.collectAsState()
    val metroAccent = rememberMetroAccentColor()

    val activeThemeInfo by themeManager.activeThemeInfo.collectAsState()
    val accentColor = when {
        activeThemeInfo.name.isMetroWp81Theme() -> metroAccent
        florisAccent.isUnspecified -> metroAccent
        else -> florisAccent
    }

    val assetResolver = remember(activeThemeInfo) {
        FlorisAssetResolver(context, activeThemeInfo)
    }
    val snyggTheme = rememberSnyggTheme(activeThemeInfo.stylesheet, assetResolver)
    val windowSpec by windowController.activeWindowSpec.collectAsState()
    val fontScale by remember { derivedStateOf { windowSpec.fontScale } }

    val state by keyboardManager.activeState.collectAsState()
    val attributes = mapOf(
        FlorisImeUi.Attr.Mode to state.keyboardMode.toString(),
        FlorisImeUi.Attr.ShiftState to state.inputShiftState.toString(),
    )

    MaterialTheme {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle.Default,
        ) {
            ProvideSnyggTheme(
                snyggTheme = snyggTheme,
                dynamicAccentColor = accentColor,
                fontSizeMultiplier = fontScale,
                assetResolver = assetResolver,
                rootAttributes = attributes,
                content = content,
                materialYouFlags = activeThemeInfo.config.materialYouFlags
            )
        }
    }
}

/**
 * Reads the suite accent from [MetroPreferences] and keeps it live when Settings broadcasts
 * [MetroBroadcasts.ACTION_THEME_CHANGED]. Pressed keys, hold popups, and emoji tabs use this.
 */
@Composable
private fun rememberMetroAccentColor(): Color {
    val context = LocalContext.current
    val prefs = remember(context) { MetroPreferences(context) }
    var accent by remember { mutableStateOf(prefs.accentColor) }

    fun reload() {
        accent = prefs.accentColor
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != MetroBroadcasts.ACTION_THEME_CHANGED) return
                val accentExtra = intent.getStringExtra(MetroBroadcasts.EXTRA_ACCENT_COLOR)
                prefs.cacheThemeSnapshot(
                    themeMode = intent.getStringExtra(MetroBroadcasts.EXTRA_THEME_MODE)
                        ?.let { MetroThemeMode.fromStorage(it) },
                    accentColorHex = accentExtra,
                )
                accentExtra?.let { hex ->
                    accent = MetroPreferences.parseAccentHex(hex)
                } ?: reload()
            }
        }
        context.registerReceiver(receiver, IntentFilter(MetroBroadcasts.ACTION_THEME_CHANGED), Context.RECEIVER_EXPORTED)
        val observer = prefs.registerObserver { reload() }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            prefs.unregisterObserver(observer)
        }
    }

    return accent
}

private fun dev.patrickgold.florisboard.lib.ext.ExtensionComponentName.isMetroWp81Theme(): Boolean {
    return extensionId == "org.florisboard.themes" && componentId in setOf("wp81_dark", "wp81_light")
}
