package com.metro.statusbar

import androidx.compose.ui.graphics.Color
import com.metro.system.MetroPreferences
import com.metro.ui.MetroColors
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TrayThemeResolver {
    fun resolve(
        preferences: MetroPreferences,
        visibilityMode: TrayVisibilityMode = TrayVisibilityMode.Opaque,
    ): TrayThemeSnapshot {
        val darkTheme = preferences.isDark
        val baseBackground = MetroColors.background(darkTheme)
        val backgroundColor = when (visibilityMode) {
            TrayVisibilityMode.Opaque -> baseBackground
            TrayVisibilityMode.Translucent -> baseBackground.copy(alpha = 0.5f)
            TrayVisibilityMode.Hidden -> Color.Transparent
        }
        return TrayThemeSnapshot(
            backgroundColor = backgroundColor,
            foregroundColor = MetroColors.primaryText(darkTheme),
            accentColor = preferences.accentColor,
            darkTheme = darkTheme,
            visibilityMode = visibilityMode,
        )
    }
}

object TrayClockFormatter {
    private val formatter = DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())

    fun format(now: ZonedDateTime = ZonedDateTime.now()): String = formatter.format(now)
}
