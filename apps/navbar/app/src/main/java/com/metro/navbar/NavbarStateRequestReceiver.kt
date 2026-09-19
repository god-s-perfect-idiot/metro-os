package com.metro.navbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroNavBar

/**
 * Answers [MetroBroadcasts.ACTION_NAVBAR_QUERY] and forwards [MetroNavBar] contract broadcasts to
 * the running overlay service.
 */
class NavbarStateRequestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (val action = intent?.action) {
            MetroBroadcasts.ACTION_NAVBAR_QUERY ->
                NavbarOverlayController.publishEnabledState(
                    context,
                    NavbarOverlayController.isActive,
                )
            MetroNavBar.ACTION_REFRESH,
            MetroNavBar.ACTION_SET_VISIBILITY,
            -> NavbarOverlayService.deliver(context, action, intent)
        }
    }
}
