package com.metro.statusbar

import android.graphics.Rect
import android.view.accessibility.AccessibilityWindowInfo

/**
 * Detects whether the Android notification shade / quick-settings panel is open.
 *
 * The Metro tray is a [TYPE_ACCESSIBILITY_OVERLAY](android.view.WindowManager.LayoutParams), so it
 * paints above SystemUI. When the shade expands, the tray must hide or it sits on top of the panel.
 *
 * Public SDK maps the shade to [AccessibilityWindowInfo.TYPE_SYSTEM] (no dedicated shade type), and
 * OEM class names vary. Detection therefore combines class/title hints with a geometry fallback:
 * the topmost non-overlay window is a large, top-anchored system window.
 */
object NotificationShadeDetector {
    /** Fraction of display height that counts as an expanded shade / system panel. */
    const val MIN_SHADE_HEIGHT_FRACTION = 0.40f

    /** Shade / panel must start near the top of the screen (fraction of height). */
    const val MAX_SHADE_TOP_FRACTION = 0.12f

    private val SystemUiPackages = setOf(
        "com.android.systemui",
    )

    /**
     * Class-name fragments on shade / notification-panel roots (AOSP + common OEM skins).
     * Keep specific — avoid bare "Panel" / "StatusBar" which match collapsed chrome.
     */
    private val ShadeClassHints = listOf(
        "NotificationShade",
        "NotificationPanel",
        "ExpandedNotification",
        "StatusBarExpanded",
        "SecNotificationPanel",
        "MiuiNotificationPanel",
        "OpNotificationPanel",
        "QuickSettings",
        "QsPanel",
    )

    private val ShadeTitleHints = listOf(
        "Notification shade",
        "Notifications",
        "Quick settings",
        "NotificationShade",
    )

    /**
     * Testable window snapshot — avoids mocking final [AccessibilityWindowInfo] in unit tests.
     */
    data class WindowProbe(
        val type: Int,
        val layer: Int,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val packageName: String? = null,
        val className: String? = null,
        val title: String? = null,
        val isActive: Boolean = false,
    ) {
        val height: Int get() = (bottom - top).coerceAtLeast(0)
    }

    fun isShadeOpen(
        windows: List<AccessibilityWindowInfo>,
        screenHeightPx: Int,
    ): Boolean {
        if (windows.isEmpty() || screenHeightPx <= 0) return false
        return isShadeOpenFromProbes(windows.map { it.toProbe() }, screenHeightPx)
    }

    fun isShadeOpenFromProbes(
        probes: List<WindowProbe>,
        screenHeightPx: Int,
    ): Boolean {
        if (probes.isEmpty() || screenHeightPx <= 0) return false

        if (probes.any { isStrongShadeSignal(it) }) return true

        val topContent = probes
            .filter { it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }
            .maxByOrNull { it.layer }
            ?: return false

        return isExpandedSystemPanel(topContent, screenHeightPx)
    }

    fun isShadeClassName(className: CharSequence?): Boolean {
        if (className.isNullOrEmpty()) return false
        val name = className.toString()
        return ShadeClassHints.any { hint -> name.contains(hint, ignoreCase = true) }
    }

    fun isShadeTitle(title: CharSequence?): Boolean {
        if (title.isNullOrEmpty()) return false
        val text = title.toString()
        return ShadeTitleHints.any { hint -> text.contains(hint, ignoreCase = true) }
    }

    fun isSystemUiPackage(packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) return false
        return packageName in SystemUiPackages || packageName.endsWith(".systemui")
    }

    /**
     * Event-path hint when SystemUI reports a window/state change before [getWindows] updates.
     */
    fun isShadeRelatedEvent(
        packageName: CharSequence?,
        className: CharSequence?,
    ): Boolean {
        if (!isSystemUiPackage(packageName?.toString())) return false
        return isShadeClassName(className) || isShadeTitle(className)
    }

    private fun isStrongShadeSignal(probe: WindowProbe): Boolean {
        if (isShadeClassName(probe.className) || isShadeTitle(probe.title)) return true
        return false
    }

    private fun isExpandedSystemPanel(probe: WindowProbe, screenHeightPx: Int): Boolean {
        if (probe.type != AccessibilityWindowInfo.TYPE_SYSTEM) return false
        val pkg = probe.packageName
        if (pkg != null && !isSystemUiPackage(pkg)) return false

        val minHeight = (screenHeightPx * MIN_SHADE_HEIGHT_FRACTION).toInt()
        val maxTop = (screenHeightPx * MAX_SHADE_TOP_FRACTION).toInt()
        if (probe.height < minHeight) return false
        if (probe.top > maxTop) return false
        // Prefer active/focused shade; still accept when SystemUI package is known (some OEMs
        // leave isActive false while the panel covers the screen).
        return probe.isActive || pkg != null
    }

    private fun AccessibilityWindowInfo.toProbe(): WindowProbe {
        val bounds = Rect()
        getBoundsInScreen(bounds)
        val root = runCatching { root }.getOrNull()
        return WindowProbe(
            type = type,
            layer = layer,
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom,
            packageName = root?.packageName?.toString(),
            className = root?.className?.toString(),
            title = title?.toString(),
            isActive = isActive || isFocused,
        ).also {
            root?.recycle()
        }
    }
}
