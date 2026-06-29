package com.metro.statusbar

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.atomic.AtomicReference

/**
 * Hosts the Metro status tray as a `TYPE_ACCESSIBILITY_OVERLAY` window.
 *
 * A plain `SYSTEM_ALERT_WINDOW` (`TYPE_APPLICATION_OVERLAY`) is always layered *below* the system
 * status bar, so the tray would be painted behind it and stay invisible. An accessibility overlay is
 * layered *above* the system bars, which is the only non-root way to draw the WP8.1 tray over the
 * Android status bar. Mirrors the navbar's accessibility-driven overlay.
 */
class StatusBarAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        instance.set(this)
        StatusBarOverlayService.onAccessibilityServiceConnected()
    }

    override fun onDestroy() {
        instance.compareAndSet(this, null)
        StatusBarOverlayService.onAccessibilityServiceDisconnected()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        private val instance = AtomicReference<StatusBarAccessibilityService?>()

        fun getInstance(): StatusBarAccessibilityService? = instance.get()

        fun isEnabled(): Boolean = instance.get() != null
    }
}
