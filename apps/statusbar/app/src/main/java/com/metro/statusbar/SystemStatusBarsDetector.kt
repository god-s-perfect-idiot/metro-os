package com.metro.statusbar

import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager

/**
 * Detects when the Android system status bars are hidden (immersive / fullscreen).
 *
 * The Metro tray is a [TYPE_ACCESSIBILITY_OVERLAY][android.view.WindowManager.LayoutParams], so it
 * stays painted above apps that go fullscreen. When system status bars are hidden, the tray must
 * hide too — matching WP8.1 `SystemTray.IsVisible = false` for fullscreen surfaces.
 *
 * Apps can also request hide via [com.metro.system.MetroStatusBar.MODE_HIDDEN]; this detector is
 * the shell safety net for immersive Android chrome (API 30+).
 */
object SystemStatusBarsDetector {
    /**
     * True when the display reports status bars as not visible.
     * Always false below API 30 ([WindowInsets.isVisible] is unavailable).
     */
    fun areHidden(wm: WindowManager): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return !wm.currentWindowMetrics.windowInsets.isVisible(WindowInsets.Type.statusBars())
    }

    /** Testable mapping from an insets-visibility flag. */
    fun areHiddenFromVisible(statusBarsVisible: Boolean?): Boolean =
        statusBarsVisible == false
}
