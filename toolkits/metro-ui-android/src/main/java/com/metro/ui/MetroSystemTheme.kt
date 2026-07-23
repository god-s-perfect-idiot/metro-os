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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroFontScale
import com.metro.system.MetroPreferences
import com.metro.system.MetroThemeMode

/**
 * Applies suite-wide theme, accent, and font scale from [MetroPreferences].
 * Observes [MetroBroadcasts.ACTION_THEME_CHANGED] so Settings writes recompose all apps.
 * Also reloads on resume and mirrors broadcast extras into the local prefs cache so cold
 * starts keep the last suite accent even if the Settings provider is briefly unreachable.
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
                val modeExtra = intent.getStringExtra(MetroBroadcasts.EXTRA_THEME_MODE)
                val accentExtra = intent.getStringExtra(MetroBroadcasts.EXTRA_ACCENT_COLOR)
                val fontExtra = if (intent.hasExtra(MetroBroadcasts.EXTRA_FONT_SCALE)) {
                    intent.getFloatExtra(MetroBroadcasts.EXTRA_FONT_SCALE, MetroFontScale.DEFAULT)
                } else {
                    null
                }
                prefs.cacheThemeSnapshot(
                    themeMode = modeExtra?.let { MetroThemeMode.fromStorage(it) },
                    accentColorHex = accentExtra,
                    fontScale = fontExtra,
                )
                modeExtra?.let { mode ->
                    darkTheme = MetroThemeMode.fromStorage(mode) == MetroThemeMode.Dark
                }
                accentExtra?.let { hex ->
                    accent = MetroPreferences.parseAccentHex(hex)
                }
                if (fontExtra != null) {
                    fontScale = MetroFontScale.coerceToStep(fontExtra)
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

    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reload()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
