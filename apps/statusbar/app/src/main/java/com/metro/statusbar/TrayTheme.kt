package com.metro.statusbar

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.metro.system.MetroPreferences
import com.metro.ui.MetroColors
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TrayThemeResolver {
    /** Luminance above this → dark (black) tray glyphs for contrast on light fills. */
    const val LIGHT_BACKGROUND_LUMINANCE = 0.5f

    fun resolve(
        preferences: MetroPreferences,
        visibilityMode: TrayVisibilityMode = TrayVisibilityMode.Opaque,
        matchAppBackground: Boolean = false,
        appBackgroundColor: Color? = null,
        /**
         * Metro suite apps ignore match-mode third-party theme colors and always use the Metro
         * page fill ([MetroColors.background]) — black in dark theme, white in light.
         */
        metroSuiteForeground: Boolean = false,
    ): TrayThemeSnapshot {
        val darkTheme = preferences.isDark
        val themeBackground = MetroColors.background(darkTheme)
        val baseBackground = when {
            metroSuiteForeground -> themeBackground
            matchAppBackground && appBackgroundColor != null -> appBackgroundColor
            else -> themeBackground
        }
        val backgroundColor = when (visibilityMode) {
            TrayVisibilityMode.Opaque -> baseBackground
            TrayVisibilityMode.Translucent -> baseBackground.copy(alpha = 0.5f)
            TrayVisibilityMode.Hidden -> Color.Transparent
        }
        val foregroundColor = when {
            metroSuiteForeground -> MetroColors.primaryText(darkTheme)
            matchAppBackground && appBackgroundColor != null -> foregroundForBackground(baseBackground)
            else -> MetroColors.primaryText(darkTheme)
        }
        return TrayThemeSnapshot(
            backgroundColor = backgroundColor,
            foregroundColor = foregroundColor,
            accentColor = preferences.accentColor,
            darkTheme = darkTheme,
            visibilityMode = visibilityMode,
        )
    }

    /** Black glyphs on light fills, white on dark — same threshold as nav-bar / tile content. */
    fun foregroundForBackground(background: Color): Color =
        if (background.luminance() > LIGHT_BACKGROUND_LUMINANCE) {
            Color.Black
        } else {
            Color.White
        }
}

object TrayClockFormatter {
    private val formatter = DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())

    fun format(now: ZonedDateTime = ZonedDateTime.now()): String = formatter.format(now)
}

/**
 * Real battery telemetry from the sticky `ACTION_BATTERY_CHANGED` intent. This is genuine device
 * data (not a stub), kept decoupled from rendering so the indicator glyph just reads a
 * [BatteryStatus].
 */
object BatterySource {
    fun parse(intent: Intent?): BatteryStatus {
        if (intent == null) return BatteryStatus.Unknown
        val present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        // Plug state is stable; status alone flickers (CHARGING ↔ NOT_CHARGING) during trickle /
        // adaptive charging while the cable stays connected.
        val charging = plugged != 0 && status != BatteryManager.BATTERY_STATUS_DISCHARGING
        return BatteryStatus.fromLevel(level, scale, charging).copy(present = present)
    }

    fun current(context: Context): BatteryStatus {
        val sticky = context.applicationContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
        return parse(sticky)
    }
}
