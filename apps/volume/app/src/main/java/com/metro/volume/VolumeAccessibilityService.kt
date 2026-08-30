package com.metro.volume

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.atomic.AtomicReference

/**
 * Filters volume rockers and hosts the volume HUD as a [TYPE_ACCESSIBILITY_OVERLAY]
 * so it can draw above the system status bar (same layering approach as statusbar/navbar).
 *
 * Keys are only consumed when [VolumeOverlayService] is alive and can adjust volume.
 * Otherwise they fall through to the system so rockers never appear "stuck."
 * Keys also fall through while the display is off or in AOD/doze so the Metro HUD never
 * covers Always-On Display; an awake lock screen is fine.
 *
 * Accessibility key filters often omit system key-repeat events, so hold-to-change is
 * implemented here with an explicit auto-repeat timer (initial delay + interval).
 */
class VolumeAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var repeatDelta = 0

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val delta = repeatDelta
            if (delta == 0) return
            if (!VolumeOverlayService.isRunning()) {
                stopRepeat()
                return
            }
            dispatchVolumeKey(delta)
            handler.postDelayed(this, VolumeHudSpec.KEY_REPEAT_INTERVAL_MS)
        }
    }

    override fun onServiceConnected() {
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info

        instance.set(this)
        // Keep the FGS up so the next rocker press can be handled (and not swallowed).
        if (VolumeHudPreferences(this).enabled && Settings.canDrawOverlays(this)) {
            runCatching { VolumeOverlayService.start(this) }
                .onFailure { Log.w(TAG, "Failed to start overlay from a11y", it) }
        }
        VolumeOverlayService.onAccessibilityServiceConnected()
    }

    override fun onDestroy() {
        stopRepeat()
        instance.compareAndSet(this, null)
        VolumeOverlayService.onAccessibilityServiceDisconnected()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }

        // Master toggle off → never consume rockers; let Android's stock HUD handle them.
        if (!VolumeHudPreferences(this).enabled) {
            stopRepeat()
            return false
        }

        // Display off / AOD → never show Metro HUD; fall through to the system.
        if (!VolumeDisplayGate.shouldPresentHud(this)) {
            stopRepeat()
            VolumeOverlayService.hideWhenDisplayAsleep()
            return false
        }

        // Ensure the overlay FGS is running; if we cannot handle the key, do not consume it.
        if (!VolumeOverlayService.isRunning()) {
            stopRepeat()
            if (!Settings.canDrawOverlays(this)) {
                return false
            }
            runCatching { VolumeOverlayService.start(this) }
                .onFailure {
                    Log.w(TAG, "Failed to start overlay for volume key", it)
                    return false
                }
            // Service starts asynchronously — let the system handle this press so volume
            // still changes; subsequent presses will hit the Metro HUD.
            return false
        }

        val delta = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) 1 else -1
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                // First press: step once and arm hold-to-repeat.
                // Later system repeats / long-press flags are consumed without re-stepping
                // so we do not double-apply alongside our timer.
                if (event.repeatCount == 0 && !event.isLongPress) {
                    val handled = dispatchVolumeKey(delta)
                    if (!handled) {
                        stopRepeat()
                        return false
                    }
                    startRepeat(delta)
                } else if (repeatDelta == 0) {
                    // Hold/repeat arrived without a prior first-press arm (e.g. FGS came
                    // up mid-hold). Step once and start our timer.
                    val handled = dispatchVolumeKey(delta)
                    if (!handled) return false
                    startRepeat(delta)
                }
                return true
            }
            KeyEvent.ACTION_UP -> {
                stopRepeat()
                return true
            }
            else -> return true
        }
    }

    private fun dispatchVolumeKey(delta: Int): Boolean {
        return runCatching { VolumeOverlayService.onVolumeKey(delta) }
            .onFailure { Log.e(TAG, "Volume key handling failed", it) }
            .getOrDefault(false)
    }

    private fun startRepeat(delta: Int) {
        stopRepeat()
        repeatDelta = delta
        handler.postDelayed(repeatRunnable, VolumeHudSpec.KEY_REPEAT_INITIAL_MS)
    }

    private fun stopRepeat() {
        handler.removeCallbacks(repeatRunnable)
        repeatDelta = 0
    }

    companion object {
        private const val TAG = "VolumeA11y"

        private val instance = AtomicReference<VolumeAccessibilityService?>()

        fun getInstance(): VolumeAccessibilityService? = instance.get()

        fun isEnabled(): Boolean = instance.get() != null
    }
}
