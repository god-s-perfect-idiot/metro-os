package com.metro.volume

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Brings the volume HUD overlay up after boot when the master toggle is on and overlay
 * permission is already granted.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!VolumeHudPreferences(context).enabled) return
        if (!Settings.canDrawOverlays(context)) return
        VolumeOverlayService.start(context)
    }
}
