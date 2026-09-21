package com.metro.lockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.metro.system.MetroLockscreen

/**
 * Exported entry point for the [MetroLockscreen] contract. Critical overlays (incoming call, alarm,
 * …) broadcast suppress / resume requests targeted at [MetroLockscreen.PACKAGE]. Widgets may
 * broadcast [MetroLockscreen.ACTION_LOCK_NOW] to lock the display via the accessibility service.
 */
class LockscreenRequestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            MetroLockscreen.ACTION_SET_SUPPRESSED ->
                LockscreenHostService.deliver(context, intent.action!!, intent)
            MetroLockscreen.ACTION_LOCK_NOW -> {
                val locked = LockscreenAccessibilityService.getInstance()?.lockScreenNow() == true
                if (!locked) {
                    Log.w(TAG, "LOCK_NOW ignored — accessibility service not connected")
                }
            }
        }
    }

    companion object {
        private const val TAG = "LockscreenRequest"
    }
}
