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
 * open or while system status bars are immersive-hidden (fullscreen).
 */
class StatusBarAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val chromePoll = Runnable { publishChromeState() }

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
        publishChromeState()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(chromePoll)
        instance.compareAndSet(this, null)
        StatusBarOverlayService.onAccessibilityServiceDisconnected()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            -> scheduleChromeProbe(event)

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            -> {
                // Content churn is noisy across apps; only care when SystemUI is the source.
                if (NotificationShadeDetector.isSystemUiPackage(event.packageName?.toString())) {
                    scheduleChromeProbe(event)
                }
            }
        }
    }

    private fun scheduleChromeProbe(event: AccessibilityEvent) {
        if (
            NotificationShadeDetector.isShadeRelatedEvent(
                packageName = event.packageName,
                className = event.className,
            )
        ) {
            StatusBarOverlayService.onNotificationShadeOpenChanged(true)
        }
        mainHandler.removeCallbacks(chromePoll)
        // Immediate probe + short follow-up — shade / immersive bounds often settle after the event.
        mainHandler.post(chromePoll)
        mainHandler.postDelayed(chromePoll, CHROME_POLL_FOLLOWUP_MS)
    }

    override fun onInterrupt() = Unit

    private fun publishChromeState() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val shadeOpen = NotificationShadeDetector.isShadeOpen(windows, screenHeightPx(wm))
        StatusBarOverlayService.onNotificationShadeOpenChanged(shadeOpen)
        StatusBarOverlayService.onSystemStatusBarsHiddenChanged(
            SystemStatusBarsDetector.areHidden(wm),
        )
    }

    private fun screenHeightPx(wm: WindowManager): Int {
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
        private const val CHROME_POLL_FOLLOWUP_MS = 120L

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
