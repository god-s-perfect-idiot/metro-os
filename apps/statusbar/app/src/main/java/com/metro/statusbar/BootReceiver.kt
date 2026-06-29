package com.metro.statusbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Brings the WP8.1 status tray up automatically after boot so the shell overlay is always present,
 * matching the system-tray behavior on Windows Phone. Only starts when the overlay permission has
 * already been granted; otherwise the user starts it from [MainActivity].
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!Settings.canDrawOverlays(context)) return
        StatusBarOverlayService.start(context)
    }
}
