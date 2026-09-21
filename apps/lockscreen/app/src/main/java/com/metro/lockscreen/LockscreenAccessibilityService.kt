package com.metro.lockscreen

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.atomic.AtomicReference

/**
 * Hosts the Metro lock surface as a [android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY]
 * so it can draw above the system keyguard (same layering as statusbar / volume / navbar).
 *
 * Also can inject a swipe-up gesture onto the keyguard as a fallback to open the SystemUI bouncer,
 * and lock the display via [GLOBAL_ACTION_LOCK_SCREEN] for suite widgets (API 28+).
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
     * Locks the display immediately (API 28+). Used by the Widgets lock tile via
     * [com.metro.system.MetroLockscreen.ACTION_LOCK_NOW].
     */
    fun lockScreenNow(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Log.w(TAG, "GLOBAL_ACTION_LOCK_SCREEN requires API 28+")
            return false
        }
        return try {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN).also { ok ->
                if (!ok) Log.w(TAG, "GLOBAL_ACTION_LOCK_SCREEN returned false")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "lockScreenNow failed", t)
            false
        }
    }

    /**
     * Swipe up on the display so SystemUI treats it like a lock-screen unlock gesture and
     * shows the PIN / pattern / password bouncer.
     *
     * Travel must be nearly full-screen and unhurried — short strokes leave SystemUI mid-swipe
     * on the decorative lock wallpaper instead of opening lock input.
     *
     * @param onResult invoked on the main thread when the stroke completes or is cancelled.
     *   Do **not** start [LockscreenBouncerActivity] after a completed gesture — that
     *   `requestDismissKeyguard` call cancels the keypad SystemUI just opened.
     */
    fun injectSwipeUpToBouncer(onResult: ((completed: Boolean) -> Unit)? = null): Boolean {
        val dm = resources.displayMetrics
        val w = dm.widthPixels.toFloat()
        val h = dm.heightPixels.toFloat()
        val path = Path().apply {
            moveTo(w / 2f, h * 0.94f)
            lineTo(w / 2f, h * 0.06f)
        }
        val stroke = GestureDescription.StrokeDescription(path, /*startTime=*/0L, /*duration=*/520L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return try {
            dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        Log.i(TAG, "swipe-up gesture completed")
                        onResult?.invoke(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Log.w(TAG, "swipe-up gesture cancelled")
                        onResult?.invoke(false)
                    }
                },
                null,
            )
        } catch (t: Throwable) {
            Log.e(TAG, "dispatchGesture failed", t)
            onResult?.invoke(false)
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
