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
    /**
     * Relative luminance above this → black tray glyphs (clock + indicators) for contrast on
     * light fills. Below 0.5 so WP accents like lime (#A4C400) and amber (#F0A30A) read as light
     * and flip to black; cyan / cobalt stay white.
     */
    const val LIGHT_BACKGROUND_LUMINANCE = 0.4f

    fun resolve(
        preferences: MetroPreferences,
        visibilityMode: TrayVisibilityMode = TrayVisibilityMode.Opaque,
        backgroundMode: StatusBarBackgroundMode = StatusBarBackgroundMode.DefaultBlackBackground,
        appBackgroundColor: Color? = null,
        /**
         * Content behind a transparent tray (sampled window top strip or app theme attrs).
         * Drives glyph contrast only — the painted band stays [Color.Transparent].
         */
        contentBackdropColor: Color? = null,
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
        // Transparent mode: prefer live/theme content backdrop; fall back to system theme.
        val logicalTransparentFill = contentBackdropColor
            ?: if (darkTheme) Color.Black else Color.White
        val matchedBackground = when (backgroundMode) {
            StatusBarBackgroundMode.DefaultBlackBackground -> Color.Black
            StatusBarBackgroundMode.ShowAccentColor -> accentBackground
            StatusBarBackgroundMode.MatchAppBackground ->
                appBackgroundColor ?: Color.Black
            StatusBarBackgroundMode.Transparent -> logicalTransparentFill
        }
        val logicalShell = shellFillColor
        val baseBackground = when {
            logicalShell != null && shellFillUnderlay -> Color.Transparent
            logicalShell != null -> logicalShell
            backgroundMode == StatusBarBackgroundMode.Transparent -> Color.Transparent
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

    /** Black glyphs on light fills, white on dark. */
    fun foregroundForBackground(background: Color): Color =
        if (isLightBackground(background)) Color.Black else Color.White

    fun isLightBackground(background: Color): Boolean =
        background.copy(alpha = 1f).luminance() > LIGHT_BACKGROUND_LUMINANCE
}

object TrayClockFormatter {
    private val formatter = DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())

    fun format(now: ZonedDateTime = ZonedDateTime.now()): String = formatter.format(now)
}

/** Minute-boundary delay for the overlay Handler tick (backup to [Intent.ACTION_TIME_TICK]). */
object TrayClockScheduler {
    fun millisUntilNextMinute(nowMillis: Long): Long {
        val remainder = nowMillis % 60_000L
        return if (remainder == 0L) 60_000L else 60_000L - remainder
    }
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
