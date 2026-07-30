package com.metro.keyboard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding

enum class KeyboardSettingsRoute {
    Root,
    Language,
    Advanced,
}

@Composable
fun KeyboardSettingsShell(
    modifier: Modifier = Modifier,
) {
    var route by remember { mutableStateOf(KeyboardSettingsRoute.Root) }
    var languageLabel by remember { mutableStateOf("English (United States)") }

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
            onOpenLanguage = { label ->
                languageLabel = label
                route = KeyboardSettingsRoute.Language
            },
            onOpenAdvanced = { route = KeyboardSettingsRoute.Advanced },
        )
        KeyboardSettingsRoute.Language -> KeyboardLanguageScreen(
            languageLabel = languageLabel,
            modifier = contentModifier,
        )
        KeyboardSettingsRoute.Advanced -> KeyboardAdvancedScreen(
            modifier = contentModifier,
        )
    }
}
