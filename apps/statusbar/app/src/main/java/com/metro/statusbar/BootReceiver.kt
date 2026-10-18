package com.metro.statusbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.metro.statusbar.shizuku.SystemStatusBarHider

/**
 * Brings the WP8.1 status tray up automatically after boot when the master toggle is on and
 * overlay permission is already granted — matching always-on system-tray behavior on Windows Phone
 * once the user has opted in from [MainActivity].
 *
 * Also re-applies Shizuku SystemUI icon hide when that preference is on (Shizuku must already be
 * running after boot — wireless-debug start is manual).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = StatusTrayPreferences(context)
        if (prefs.hideSystemStatusBar) {
            SystemStatusBarHider.applyDesiredAsync(context)
        }
        if (!prefs.enabled) return
        if (!Settings.canDrawOverlays(context)) return
        StatusBarOverlayService.start(context)
    }
}
