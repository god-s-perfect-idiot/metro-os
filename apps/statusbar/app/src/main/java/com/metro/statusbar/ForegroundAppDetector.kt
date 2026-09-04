package com.metro.statusbar

import android.view.accessibility.AccessibilityWindowInfo

/**
 * Picks the foreground application package from accessibility windows so the tray can match that
 * app's background color.
 */
object ForegroundAppDetector {
    private val IgnoredPackages = setOf(
        "com.android.systemui",
        "com.metro.navbar",
        "com.metro.notifications",
        "com.metro.volume",
        "com.metro.lockscreen",
    )

    /**
     * Returns the topmost application window package, skipping shell overlays and SystemUI.
     */
    fun foregroundPackage(windows: List<AccessibilityWindowInfo>): String? {
        if (windows.isEmpty()) return null
        return foregroundPackageFromProbes(windows.map { it.toPackageProbe() })
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

    private fun AccessibilityWindowInfo.toPackageProbe(): PackageProbe {
        val root = runCatching { root }.getOrNull()
        val packageName = root?.packageName?.toString()
        root?.recycle()
        return PackageProbe(
            type = type,
            layer = layer,
            packageName = packageName,
            isActive = isActive,
            isFocused = isFocused,
        )
    }
}
