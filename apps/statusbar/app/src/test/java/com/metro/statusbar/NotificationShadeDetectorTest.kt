package com.metro.statusbar

import android.view.accessibility.AccessibilityWindowInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationShadeDetectorTest {
    private val screenHeight = 1280

    @Test
    fun isShadeClassName_matchesAospShadeRoots() {
        assertTrue(
            NotificationShadeDetector.isShadeClassName(
                "com.android.systemui.statusbar.phone.NotificationShadeWindowView",
            ),
        )
        assertTrue(
            NotificationShadeDetector.isShadeClassName(
                "com.android.systemui.shade.NotificationShadeWindowView",
            ),
        )
        assertTrue(
            NotificationShadeDetector.isShadeClassName(
                "com.android.systemui.statusbar.phone.NotificationPanelView",
            ),
        )
    }

    @Test
    fun isShadeClassName_rejectsCollapsedStatusBarChrome() {
        assertFalse(
            NotificationShadeDetector.isShadeClassName(
                "com.android.systemui.statusbar.phone.StatusBarWindowView",
            ),
        )
        assertFalse(NotificationShadeDetector.isShadeClassName("android.widget.FrameLayout"))
        assertFalse(NotificationShadeDetector.isShadeClassName(null))
        assertFalse(NotificationShadeDetector.isShadeClassName(""))
    }

    @Test
    fun isShadeOpen_emptyWindows_isClosed() {
        assertFalse(NotificationShadeDetector.isShadeOpenFromProbes(emptyList(), screenHeight))
    }

    @Test
    fun isShadeOpen_strongClassName_isOpen() {
        val probes = listOf(
            appWindow(layer = 1),
            NotificationShadeDetector.WindowProbe(
                type = AccessibilityWindowInfo.TYPE_SYSTEM,
                layer = 10,
                left = 0,
                top = 0,
                right = 720,
                bottom = 100,
                packageName = "com.android.systemui",
                className = "com.android.systemui.shade.NotificationShadeWindowView",
            ),
        )
        assertTrue(NotificationShadeDetector.isShadeOpenFromProbes(probes, screenHeight))
    }

    @Test
    fun isShadeOpen_topFullscreenSystemUi_isOpen() {
        val probes = listOf(
            appWindow(layer = 1),
            NotificationShadeDetector.WindowProbe(
                type = AccessibilityWindowInfo.TYPE_SYSTEM,
                layer = 20,
                left = 0,
                top = 0,
                right = 720,
                bottom = screenHeight,
                packageName = "com.android.systemui",
                isActive = true,
            ),
        )
        assertTrue(NotificationShadeDetector.isShadeOpenFromProbes(probes, screenHeight))
    }

    @Test
    fun isShadeOpen_collapsedStatusBarStrip_isClosed() {
        val probes = listOf(
            appWindow(layer = 10),
            NotificationShadeDetector.WindowProbe(
                type = AccessibilityWindowInfo.TYPE_SYSTEM,
                layer = 5,
                left = 0,
                top = 0,
                right = 720,
                bottom = 72,
                packageName = "com.android.systemui",
                className = "com.android.systemui.statusbar.phone.StatusBarWindowView",
                isActive = false,
            ),
        )
        assertFalse(NotificationShadeDetector.isShadeOpenFromProbes(probes, screenHeight))
    }

    @Test
    fun isShadeOpen_applicationOnTop_isClosed() {
        val probes = listOf(
            NotificationShadeDetector.WindowProbe(
                type = AccessibilityWindowInfo.TYPE_SYSTEM,
                layer = 5,
                left = 0,
                top = 0,
                right = 720,
                bottom = screenHeight,
                packageName = "com.android.systemui",
            ),
            appWindow(layer = 30),
        )
        assertFalse(NotificationShadeDetector.isShadeOpenFromProbes(probes, screenHeight))
    }

    @Test
    fun isShadeRelatedEvent_requiresSystemUiPackage() {
        assertTrue(
            NotificationShadeDetector.isShadeRelatedEvent(
                packageName = "com.android.systemui",
                className = "com.android.systemui.shade.NotificationShadeWindowView",
            ),
        )
        assertFalse(
            NotificationShadeDetector.isShadeRelatedEvent(
                packageName = "com.metro.launcher",
                className = "com.android.systemui.shade.NotificationShadeWindowView",
            ),
        )
    }

    private fun appWindow(layer: Int) = NotificationShadeDetector.WindowProbe(
        type = AccessibilityWindowInfo.TYPE_APPLICATION,
        layer = layer,
        left = 0,
        top = 0,
        right = 720,
        bottom = screenHeight,
        packageName = "com.metro.launcher",
        isActive = true,
    )
}
