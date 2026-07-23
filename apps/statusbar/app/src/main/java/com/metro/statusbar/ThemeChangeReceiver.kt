package com.metro.statusbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroFontScale
import com.metro.system.MetroPreferences
import com.metro.system.MetroThemeMode

class ThemeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != MetroBroadcasts.ACTION_THEME_CHANGED) return
        MetroPreferences(context).cacheThemeSnapshot(
            themeMode = intent.getStringExtra(MetroBroadcasts.EXTRA_THEME_MODE)
                ?.let { MetroThemeMode.fromStorage(it) },
            accentColorHex = intent.getStringExtra(MetroBroadcasts.EXTRA_ACCENT_COLOR),
            fontScale = if (intent.hasExtra(MetroBroadcasts.EXTRA_FONT_SCALE)) {
                intent.getFloatExtra(MetroBroadcasts.EXTRA_FONT_SCALE, MetroFontScale.DEFAULT)
            } else {
                null
            },
        )
        StatusBarOverlayService.requestRefresh(context)
    }
}
