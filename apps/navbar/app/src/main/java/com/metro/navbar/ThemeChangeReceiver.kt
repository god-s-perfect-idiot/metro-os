package com.metro.navbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ThemeChangeReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action != com.metro.system.MetroBroadcasts.ACTION_THEME_CHANGED) return
    NavbarOverlayService.requestRefresh(context)
  }
}
