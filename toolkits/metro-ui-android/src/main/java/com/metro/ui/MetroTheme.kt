package com.metro.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class MetroThemeColors(
    val background: Color,
    val secondarySurface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val accent: Color,
    val chromeBackground: Color,
)

val LocalMetroThemeColors = staticCompositionLocalOf {
    MetroThemeColors(
        background = MetroColors.DarkBackground,
        secondarySurface = MetroColors.DarkSecondarySurface,
        primaryText = MetroColors.DarkPrimaryText,
        secondaryText = MetroColors.DarkSecondaryText,
        accent = MetroColors.AccentBlue,
        chromeBackground = MetroColors.chromeBackground(dark = true),
    )
}

object MetroTheme {
    val colors: MetroThemeColors
        @Composable get() = LocalMetroThemeColors.current
}

@Composable
fun MetroTheme(
    darkTheme: Boolean = true,
    accent: Color = MetroColors.AccentBlue,
    content: @Composable () -> Unit,
) {
    val colors = MetroThemeColors(
        background = MetroColors.background(darkTheme),
        secondarySurface = MetroColors.secondarySurface(darkTheme),
        primaryText = MetroColors.primaryText(darkTheme),
        secondaryText = MetroColors.secondaryText(darkTheme),
        accent = accent,
        chromeBackground = MetroColors.chromeBackground(darkTheme),
    )
    CompositionLocalProvider(LocalMetroThemeColors provides colors, content = content)
}
