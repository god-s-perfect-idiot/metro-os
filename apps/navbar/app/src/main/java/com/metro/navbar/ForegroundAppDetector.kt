package com.metro.navbar

import android.view.accessibility.AccessibilityWindowInfo

/**
 * Picks the foreground application package from accessibility windows so the navbar can match that
 * app's background color.
 *
 * **ANR note:** [AccessibilityWindowInfo.getRoot] is a blocking Binder call. Never probe every
 * window on the main thread — prefer a single focused/active application window, and prefer
 * [android.view.accessibility.AccessibilityEvent.getPackageName] when the event already names
 * the app.
 */
object ForegroundAppDetector {
    private val IgnoredPackages = setOf(
        "com.android.systemui",
        "com.metro.statusbar",
        "com.metro.notifications",
        "com.metro.volume",
        "com.metro.lockscreen",
        "com.metro.navbar",
    )

    /**
     * Returns the topmost application window package, skipping shell overlays and SystemUI.
     *
     * Probes at most a handful of application windows (focused/active first) so chrome polls do
     * not ANR waiting on [AccessibilityWindowInfo.getRoot].
     */
    fun foregroundPackage(windows: List<AccessibilityWindowInfo>): String? {
        if (windows.isEmpty()) return null
        val application = windows
            .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            .sortedWith(
                compareByDescending<AccessibilityWindowInfo> { it.isActive || it.isFocused }
                    .thenByDescending { it.layer },
            )
        for (window in application) {
            val pkg = window.packageNameOrNull()
            if (!isIgnored(pkg)) return pkg
        }
        // Fallback: highest non-shell / non-system overlay (still one getRoot at a time).
        val fallback = windows
            .filter { it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }
            .filter { it.type != AccessibilityWindowInfo.TYPE_SYSTEM }
            .sortedByDescending { it.layer }
        for (window in fallback) {
            val pkg = window.packageNameOrNull()
            if (!isIgnored(pkg)) return pkg
        }
        return null
    }

    fun foregroundPackageFromProbes(probes: List<PackageProbe>): String? {
        if (probes.isEmpty()) return null
        val application = probes
            .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            .filter { !isIgnored(it.packageName) }
            .sortedWith(
                compareByDescending<PackageProbe> { it.isActive || it.isFocused }
                    .thenByDescending { it.layer },
            )
        // Prefer Start when it is already active/focused — outgoing apps can briefly keep a
        // higher layer during CLEAR_TOP and otherwise win the sort.
        application
            .firstOrNull {
                it.packageName == NavbarSpec.LAUNCHER_PACKAGE && (it.isActive || it.isFocused)
            }
            ?.packageName
            ?.let { return it }
        return application.firstOrNull()?.packageName
            ?: probes
                .filter { !isIgnored(it.packageName) }
                .filter { it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }
                .filter { it.type != AccessibilityWindowInfo.TYPE_SYSTEM }
                .maxByOrNull { it.layer }
                ?.packageName
    }

    fun isIgnored(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return true
        if (packageName in IgnoredPackages) return true
        if (packageName.endsWith(".systemui")) return true
        return false
    }

    data class PackageProbe(
        val type: Int,
        val layer: Int,
        val packageName: String?,
        val isActive: Boolean = false,
        val isFocused: Boolean = false,
    )

    private fun AccessibilityWindowInfo.packageNameOrNull(): String? {
        val root = runCatching { root }.getOrNull() ?: return null
        return try {
            root.packageName?.toString()
        } finally {
            root.recycle()
        }
    }
}
