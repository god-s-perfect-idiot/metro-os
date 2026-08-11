package com.metro.notifications

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.atomic.AtomicReference

/**
 * Hosts toast banners as `TYPE_ACCESSIBILITY_OVERLAY` so they draw above the system
 * status bar (same layering as statusbar / volume).
 */
class NotificationsAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        instance.set(this)
        NotificationsOverlayService.onAccessibilityServiceConnected()
    }

    override fun onDestroy() {
        instance.compareAndSet(this, null)
        NotificationsOverlayService.onAccessibilityServiceDisconnected()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        private val instance = AtomicReference<NotificationsAccessibilityService?>()

        fun getInstance(): NotificationsAccessibilityService? = instance.get()

        fun isEnabled(): Boolean = instance.get() != null
    }
}
