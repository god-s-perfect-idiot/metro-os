package com.metro.statusbar

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.metro.system.MetroPreferences
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TrayThemeResolver {
    /** Luminance above this → dark (black) tray glyphs for contrast on light fills. */
    const val LIGHT_BACKGROUND_LUMINANCE = 0.5f

    fun resolve(
        preferences: MetroPreferences,
        visibilityMode: TrayVisibilityMode = TrayVisibilityMode.Opaque,
        backgroundMode: StatusBarBackgroundMode = StatusBarBackgroundMode.DefaultBlackBackground,
        appBackgroundColor: Color? = null,
        /**
         * Temporary fill from a top shell overlay (toast accent / volume charcoal). Wins over
         * theme and status-bar background mode so the tray and overlay read as one band.
         */
        shellFillColor: Color? = null,
        /**
         * When true, the shell overlay paints the continuous fill under the tray (volume wipe).
         * The tray stays transparent so glyphs float on that underlay — avoids a hard seam
         * between two separately animated windows.
         */
        shellFillUnderlay: Boolean = false,
    ): TrayThemeSnapshot {
        val darkTheme = preferences.isDark
        val accentBackground = preferences.accentColor
        val matchedBackground = when (backgroundMode) {
            StatusBarBackgroundMode.DefaultBlackBackground -> Color.Black
            StatusBarBackgroundMode.ShowAccentColor -> accentBackground
            StatusBarBackgroundMode.MatchAppBackground ->
                appBackgroundColor ?: Color.Black
        }
        val logicalShell = shellFillColor
        val baseBackground = when {
            logicalShell != null && shellFillUnderlay -> Color.Transparent
            logicalShell != null -> logicalShell
            else -> matchedBackground
        }
        val backgroundColor = when (visibilityMode) {
            TrayVisibilityMode.Opaque -> baseBackground
            TrayVisibilityMode.Translucent ->
                if (baseBackground == Color.Transparent) {
                    Color.Transparent
                } else {
                    baseBackground.copy(alpha = 0.5f)
                }
            TrayVisibilityMode.Hidden -> Color.Transparent
        }
        val foregroundColor = when {
            logicalShell != null -> foregroundForBackground(logicalShell)
            else -> foregroundForBackground(matchedBackground)
        }
        return TrayThemeSnapshot(
            backgroundColor = backgroundColor,
            foregroundColor = foregroundColor,
            accentColor = preferences.accentColor,
            darkTheme = darkTheme,
            visibilityMode = visibilityMode,
            backdropColor = when {
                logicalShell != null && shellFillUnderlay -> logicalShell
                backgroundColor == Color.Transparent -> matchedBackground
                else -> backgroundColor
            },
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
