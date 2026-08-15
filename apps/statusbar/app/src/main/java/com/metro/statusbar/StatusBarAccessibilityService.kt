package com.metro.statusbar

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.atomic.AtomicReference

/**
 * Hosts the Metro status tray as a `TYPE_ACCESSIBILITY_OVERLAY` window.
 *
 * A plain `SYSTEM_ALERT_WINDOW` (`TYPE_APPLICATION_OVERLAY`) is always layered *below* the system
 * status bar, so the tray would be painted behind it and stay invisible. An accessibility overlay is
 * layered *above* the system bars, which is the only non-root way to draw the WP8.1 tray over the
 * Android status bar. Mirrors the navbar's accessibility-driven overlay.
 *
 * Also watches interactive windows so the tray can hide while the Android notification shade is
 * open (the overlay would otherwise sit on top of the shade).
 */
class StatusBarAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val shadePoll = Runnable { publishShadeState() }

    override fun onServiceConnected() {
        instance.set(this)
        // Force interactive-window retrieval even if a cached service config predates the XML
        // update — otherwise getWindows() is empty and shade-open detection never fires.
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 16
        }
        StatusBarOverlayService.onAccessibilityServiceConnected()
        publishShadeState()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(shadePoll)
        instance.compareAndSet(this, null)
        StatusBarOverlayService.onAccessibilityServiceDisconnected()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            -> scheduleShadeProbe(event)

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            -> {
                // Content churn is noisy across apps; only care when SystemUI is the source.
                if (NotificationShadeDetector.isSystemUiPackage(event.packageName?.toString())) {
                    scheduleShadeProbe(event)
                }
            }
        }
    }

    private fun scheduleShadeProbe(event: AccessibilityEvent) {
        if (
            NotificationShadeDetector.isShadeRelatedEvent(
                packageName = event.packageName,
                className = event.className,
            )
        ) {
            StatusBarOverlayService.onNotificationShadeOpenChanged(true)
        }
        mainHandler.removeCallbacks(shadePoll)
        // Immediate probe + short follow-up — shade bounds often settle after the event.
        mainHandler.post(shadePoll)
        mainHandler.postDelayed(shadePoll, SHADE_POLL_FOLLOWUP_MS)
    }

    override fun onInterrupt() = Unit

    private fun publishShadeState() {
        val open = NotificationShadeDetector.isShadeOpen(windows, screenHeightPx())
        StatusBarOverlayService.onNotificationShadeOpenChanged(open)
    }

    private fun screenHeightPx(): Int {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            wm.currentWindowMetrics.bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            metrics.heightPixels
        }
    }

    companion object {
        private const val SHADE_POLL_FOLLOWUP_MS = 120L

        private val instance = AtomicReference<StatusBarAccessibilityService?>()

        fun getInstance(): StatusBarAccessibilityService? = instance.get()

        fun isEnabled(): Boolean = instance.get() != null

        /** Opens the Android notification shade (swipe-down equivalent). */
        fun openNotificationShade(): Boolean {
            val service = instance.get() ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }
    }
}
