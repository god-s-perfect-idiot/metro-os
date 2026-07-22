package com.metro.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroFontScale
import com.metro.system.MetroPreferences
import com.metro.system.MetroThemeMode

/**
 * Applies suite-wide theme, accent, and font scale from [MetroPreferences].
 * Observes [MetroBroadcasts.ACTION_THEME_CHANGED] so Settings writes recompose all apps.
 */
@Composable
fun MetroSystemTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember(context) { MetroPreferences(context) }

    var darkTheme by remember { mutableStateOf(prefs.isDark) }
    var accent by remember { mutableStateOf(prefs.accentColor) }
    var fontScale by remember { mutableFloatStateOf(prefs.fontScale) }

    fun reload() {
        darkTheme = prefs.isDark
        accent = prefs.accentColor
        fontScale = prefs.fontScale
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != MetroBroadcasts.ACTION_THEME_CHANGED) return
                intent.getStringExtra(MetroBroadcasts.EXTRA_THEME_MODE)?.let { mode ->
                    darkTheme = MetroThemeMode.fromStorage(mode) == MetroThemeMode.Dark
                }
                intent.getStringExtra(MetroBroadcasts.EXTRA_ACCENT_COLOR)?.let { hex ->
                    accent = MetroPreferences.parseAccentHex(hex)
                }
                if (intent.hasExtra(MetroBroadcasts.EXTRA_FONT_SCALE)) {
                    fontScale = MetroFontScale.coerceToStep(
                        intent.getFloatExtra(MetroBroadcasts.EXTRA_FONT_SCALE, MetroFontScale.DEFAULT),
                    )
                } else {
                    reload()
                }
            }
        }
        val filter = IntentFilter(MetroBroadcasts.ACTION_THEME_CHANGED)
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        val observer = prefs.registerObserver { reload() }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            prefs.unregisterObserver(observer)
        }
    }

    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, fontScale) {
        Density(
            density = baseDensity.density,
            fontScale = fontScale,
        )
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MetroTheme(darkTheme = darkTheme, accent = accent, content = content)
    }
}
