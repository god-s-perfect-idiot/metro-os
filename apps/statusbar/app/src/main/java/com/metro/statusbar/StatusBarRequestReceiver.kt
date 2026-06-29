package com.metro.statusbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.metro.system.MetroStatusBar

/**
 * Exported entry point for the [MetroStatusBar] contract. Other Metro apps broadcast tray requests
 * (progress, visibility, refresh) targeted at [MetroStatusBar.PACKAGE] without a classpath
 * dependency on this app; this receiver forwards them to the running overlay service.
 *
 * Mirrors the navbar's request-receiver pattern (scope.md § Inter-app communication).
 */
class StatusBarRequestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (val action = intent?.action) {
            MetroStatusBar.ACTION_REFRESH,
            MetroStatusBar.ACTION_SET_PROGRESS,
            MetroStatusBar.ACTION_SET_VISIBILITY,
            -> StatusBarOverlayService.deliver(context, action, intent)
        }
    }
}
