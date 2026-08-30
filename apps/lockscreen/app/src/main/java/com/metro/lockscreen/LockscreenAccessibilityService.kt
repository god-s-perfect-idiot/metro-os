package com.metro.lockscreen

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.atomic.AtomicReference

/**
 * Hosts the Metro lock surface as a [android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY]
 * so it can draw above the system keyguard (same layering as statusbar / volume / navbar).
 *
 * Also can inject a swipe-up gesture onto the keyguard as a fallback to open the SystemUI bouncer.
 */
class LockscreenAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        instance.set(this)
        LockscreenHostService.onAccessibilityServiceConnected()
    }

    override fun onDestroy() {
        instance.compareAndSet(this, null)
        LockscreenHostService.onAccessibilityServiceDisconnected()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    /**
     * Swipe up on the display so SystemUI treats it like a lock-screen unlock gesture and
     * shows the PIN / pattern / password bouncer.
     */
    fun injectSwipeUpToBouncer(): Boolean {
        val dm = resources.displayMetrics
        val w = dm.widthPixels.toFloat()
        val h = dm.heightPixels.toFloat()
        val path = Path().apply {
            moveTo(w / 2f, h * 0.85f)
            lineTo(w / 2f, h * 0.25f)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 280L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return try {
            dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        Log.i(TAG, "swipe-up gesture completed")
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Log.w(TAG, "swipe-up gesture cancelled")
                    }
                },
                null,
            )
        } catch (t: Throwable) {
            Log.e(TAG, "dispatchGesture failed", t)
            false
        }
    }

    companion object {
        private const val TAG = "LockscreenA11y"

        private val instance = AtomicReference<LockscreenAccessibilityService?>()

        fun getInstance(): LockscreenAccessibilityService? = instance.get()

        fun isEnabled(): Boolean = instance.get() != null
    }
}
