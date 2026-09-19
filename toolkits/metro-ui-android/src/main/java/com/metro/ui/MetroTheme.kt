package com.metro.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

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

/** Suite chrome typeface from Settings → start+theme (via [MetroSystemTheme]). */
val LocalMetroFontFamily = staticCompositionLocalOf { MetroFontFamily }

object MetroTheme {
    val colors: MetroThemeColors
        @Composable get() = LocalMetroThemeColors.current

    val fontFamily: FontFamily
        @Composable get() = LocalMetroFontFamily.current
}

@Composable
fun MetroTheme(
    darkTheme: Boolean = true,
    accent: Color = MetroColors.AccentBlue,
    fontFamily: FontFamily = MetroFontFamily,
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
    CompositionLocalProvider(
        LocalMetroThemeColors provides colors,
        LocalMetroFontFamily provides fontFamily,
        content = content,
    )
}
