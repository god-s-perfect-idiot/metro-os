package com.metro.statusbar

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
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
