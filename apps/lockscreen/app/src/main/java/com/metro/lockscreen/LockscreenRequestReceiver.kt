package com.metro.lockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.metro.system.MetroLockscreen

/**
 * Exported entry point for the [MetroLockscreen] contract. Critical overlays (incoming call, alarm,
 * …) broadcast suppress / resume requests targeted at [MetroLockscreen.PACKAGE].
 */
class LockscreenRequestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            MetroLockscreen.ACTION_SET_SUPPRESSED ->
                LockscreenHostService.deliver(context, intent.action!!, intent)
        }
    }
}
