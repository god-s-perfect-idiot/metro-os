package com.metro.navbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.metro.system.MetroBroadcasts

/**
 * Answers [MetroBroadcasts.ACTION_NAVBAR_QUERY] so apps can learn the current navbar state on
 * cold start (when they may have missed the live [MetroBroadcasts.ACTION_NAVBAR_CHANGED] signal).
 * Replies by re-broadcasting the current enabled state.
 */
class NavbarStateRequestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != MetroBroadcasts.ACTION_NAVBAR_QUERY) return
        NavbarOverlayController.publishEnabledState(context, NavbarOverlayController.isActive)
    }
}
