package com.metro.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!NotificationsPreferences(context).enabled) return
        if (!Settings.canDrawOverlays(context)) return
        NotificationsOverlayService.start(context)
    }
}
