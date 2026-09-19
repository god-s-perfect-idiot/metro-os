package com.metro.navbar

import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager

/**
 * Detects when the Android system navigation bars are hidden (immersive / fullscreen).
 *
 * The Metro bar is a [TYPE_ACCESSIBILITY_OVERLAY][android.view.WindowManager.LayoutParams], so it
 * stays painted above apps that go fullscreen. When system navigation bars are hidden, the Metro
 * bar must hide too — matching WP8.1 soft-key chrome for fullscreen surfaces.
 *
 * Apps can also request hide via [com.metro.system.MetroNavBar.MODE_HIDDEN]; this detector is the
 * shell safety net for immersive Android chrome (API 30+).
 */
object SystemNavigationBarsDetector {
    /**
     * True when the display reports navigation bars as not visible.
     * Always false below API 30 ([WindowInsets.isVisible] is unavailable).
     */
    fun areHidden(wm: WindowManager): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return !wm.currentWindowMetrics.windowInsets.isVisible(WindowInsets.Type.navigationBars())
    }

    /** Testable mapping from an insets-visibility flag. */
    fun areHiddenFromVisible(navigationBarsVisible: Boolean?): Boolean =
        navigationBarsVisible == false
}
