package com.metro.lockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Brings the lock-screen host up after boot when the master toggle is on.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!LockscreenPreferences(context).enabled) return
        LockscreenHostService.start(context)
    }
}
