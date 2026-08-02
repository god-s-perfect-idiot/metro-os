package com.metro.volume

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
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
 */
class VolumeAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info

        instance.set(this)
        // Keep the FGS up so the next rocker press can be handled (and not swallowed).
        if (Settings.canDrawOverlays(this)) {
            runCatching { VolumeOverlayService.start(this) }
                .onFailure { Log.w(TAG, "Failed to start overlay from a11y", it) }
        }
        VolumeOverlayService.onAccessibilityServiceConnected()
    }

    override fun onDestroy() {
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

        // Ensure the overlay FGS is running; if we cannot handle the key, do not consume it.
        if (!VolumeOverlayService.isRunning()) {
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

        if (event.action == KeyEvent.ACTION_DOWN && !event.isLongPress) {
            val delta = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) 1 else -1
            val handled = runCatching { VolumeOverlayService.onVolumeKey(delta) }
                .onFailure { Log.e(TAG, "Volume key handling failed", it) }
                .getOrDefault(false)
            if (!handled) return false
        }
        // Consume up/down only while the overlay is known to be running.
        return true
    }

    companion object {
        private const val TAG = "VolumeA11y"

        private val instance = AtomicReference<VolumeAccessibilityService?>()

        fun getInstance(): VolumeAccessibilityService? = instance.get()

        fun isEnabled(): Boolean = instance.get() != null
    }
}
