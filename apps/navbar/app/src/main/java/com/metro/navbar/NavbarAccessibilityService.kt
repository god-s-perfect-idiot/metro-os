package com.metro.navbar

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Hosts the Metro navigation bar as a `TYPE_ACCESSIBILITY_OVERLAY` window and watches chrome
 * state so the bar can hide for immersive fullscreen and match the foreground app fill.
 *
 * Window-root probing ([android.view.accessibility.AccessibilityWindowInfo.getRoot]) is a
 * blocking Binder call — never run it on the main thread (see dropbox `data_app_anr` stacks).
 */
class NavbarAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null
    private val probeForegroundOnNextPoll = AtomicBoolean(false)

    private val scheduleChromePoll = Runnable {
        enqueueChromePoll()
    }

    override fun onServiceConnected() {
        instance.set(this)
        HandlerThread("navbar-chrome").also { thread ->
            thread.start()
            workerThread = thread
            workerHandler = Handler(thread.looper)
        }
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            // Debounce window chatter — a 16ms timeout previously flooded getRoot() polls.
            notificationTimeout = CHROME_EVENT_DEBOUNCE_MS
        }
        NavbarOverlayController.onAccessibilityServiceConnected(this)
        probeForegroundOnNextPoll.set(true)
        enqueueChromePoll()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(scheduleChromePoll)
        workerHandler?.removeCallbacksAndMessages(null)
        workerThread?.quitSafely()
        workerHandler = null
        workerThread = null
        NavbarOverlayController.onAccessibilityServiceDisconnected()
        instance.compareAndSet(this, null)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Fast path: event already names the app — no getRoot(). Authoritative so a
                // concurrent windows probe cannot re-apply the previous package (e.g. Start).
                val pkg = event.packageName?.toString()
                if (!ForegroundAppDetector.isIgnored(pkg)) {
                    NavbarOverlayService.onForegroundPackageChanged(pkg, authoritative = true)
                }
                scheduleChromePoll(probeForeground = false)
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                // Immersive / multi-window — refresh nav-bar visibility; probe package off-main
                // only when needed (event often lacks a reliable package).
                scheduleChromePoll(probeForeground = true)
            }
        }
    }

    override fun onInterrupt() = Unit

    private fun scheduleChromePoll(probeForeground: Boolean) {
        if (probeForeground) {
            probeForegroundOnNextPoll.set(true)
        }
        mainHandler.removeCallbacks(scheduleChromePoll)
        mainHandler.postDelayed(scheduleChromePoll, CHROME_POLL_DEBOUNCE_MS)
    }

    private fun enqueueChromePoll() {
        val handler = workerHandler ?: return
        handler.removeCallbacksAndMessages(null)
        // Capture epoch before probing so a Start / window-state update during getRoot() wins.
        val epochAtStart = NavbarOverlayController.currentForegroundEpoch()
        handler.post {
            val probeForeground = probeForegroundOnNextPoll.getAndSet(false)
            val hidden = runCatching {
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                SystemNavigationBarsDetector.areHidden(wm)
            }.getOrDefault(false)
            val pkg = if (probeForeground) {
                runCatching { ForegroundAppDetector.foregroundPackage(windows) }.getOrNull()
            } else {
                null
            }
            mainHandler.post {
                NavbarOverlayService.onSystemNavigationBarsHiddenChanged(hidden)
                // Inconclusive null must not clear a known foreground (keeps Start black).
                if (probeForeground && pkg != null) {
                    NavbarOverlayService.onForegroundPackageChanged(
                        pkg,
                        epoch = epochAtStart,
                        fromProbe = true,
                    )
                }
            }
        }
    }

    companion object {
        private const val CHROME_EVENT_DEBOUNCE_MS = 100L
        private const val CHROME_POLL_DEBOUNCE_MS = 200L

        private val instance = AtomicReference<NavbarAccessibilityService?>()

        fun getInstance(): NavbarAccessibilityService? = instance.get()

        fun performBack(): Boolean {
            val service = instance.get() ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_BACK)
        }

        fun performRecents(): Boolean {
            val service = instance.get() ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_RECENTS)
        }

        fun isEnabled(): Boolean = instance.get() != null
    }
}
