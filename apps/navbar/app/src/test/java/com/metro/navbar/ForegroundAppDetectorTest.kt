package com.metro.navbar

import android.view.accessibility.AccessibilityWindowInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundAppDetectorTest {
    @Test
    fun foregroundPackage_prefersActiveApplicationWindow() {
        val pkg = ForegroundAppDetector.foregroundPackageFromProbes(
            listOf(
                ForegroundAppDetector.PackageProbe(
                    type = AccessibilityWindowInfo.TYPE_SYSTEM,
                    layer = 100,
                    packageName = "com.android.systemui",
                    isActive = true,
                    isFocused = true,
                ),
                ForegroundAppDetector.PackageProbe(
                    type = AccessibilityWindowInfo.TYPE_APPLICATION,
                    layer = 10,
                    packageName = "com.example.photos",
                    isActive = true,
                    isFocused = true,
                ),
                ForegroundAppDetector.PackageProbe(
                    type = AccessibilityWindowInfo.TYPE_APPLICATION,
                    layer = 5,
                    packageName = "com.example.notes",
                    isActive = false,
                    isFocused = false,
                ),
            ),
        )
        assertEquals("com.example.photos", pkg)
    }

    @Test
    fun foregroundPackage_prefersActiveLauncherOverHigherLayerOutgoingApp() {
        val pkg = ForegroundAppDetector.foregroundPackageFromProbes(
            listOf(
                ForegroundAppDetector.PackageProbe(
                    type = AccessibilityWindowInfo.TYPE_APPLICATION,
                    layer = 40,
                    packageName = "com.example.photos",
                    isActive = false,
                    isFocused = false,
                ),
                ForegroundAppDetector.PackageProbe(
                    type = AccessibilityWindowInfo.TYPE_APPLICATION,
                    layer = 10,
                    packageName = NavbarSpec.LAUNCHER_PACKAGE,
                    isActive = true,
                    isFocused = true,
                ),
            ),
        )
        assertEquals(NavbarSpec.LAUNCHER_PACKAGE, pkg)
    }

    @Test
    fun isIgnored_shellAndSystemUi() {
        assertTrue(ForegroundAppDetector.isIgnored("com.metro.navbar"))
        assertTrue(ForegroundAppDetector.isIgnored("com.metro.statusbar"))
        assertTrue(ForegroundAppDetector.isIgnored("com.android.systemui"))
        assertTrue(ForegroundAppDetector.isIgnored(null))
        assertNull(
            ForegroundAppDetector.foregroundPackageFromProbes(
                listOf(
                    ForegroundAppDetector.PackageProbe(
                        type = AccessibilityWindowInfo.TYPE_APPLICATION,
                        layer = 1,
                        packageName = "com.metro.navbar",
                    ),
                ),
            ),
        )
    }
}
