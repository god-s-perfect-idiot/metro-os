package com.metro.lockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Reloads theme-dependent presentation when the suite accent / dark-light theme changes.
 * The PoC lock fill uses accent at compose time; setup UI refreshes on next resume.
 */
class ThemeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != "com.metro.system.THEME_CHANGED") return
        // Rebuild overlay so accent fill / chrome contrast pick up the new theme.
        LockscreenHostService.requestRehost()
    }
}
