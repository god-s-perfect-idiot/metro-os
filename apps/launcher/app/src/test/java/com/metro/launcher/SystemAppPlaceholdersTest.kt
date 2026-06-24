package com.metro.launcher

import com.metro.launcher.data.PinnedTileStore
import com.metro.launcher.data.SystemAppPlaceholders
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemAppPlaceholdersTest {
    @Test
    fun defaultPins_havePlaceholderIcons() {
        PinnedTileStore.defaultPins().forEach { pin ->
            assertNotNull(
                "Missing placeholder for ${pin.packageName}",
                SystemAppPlaceholders.iconResId(pin.packageName),
            )
            assertNotNull(
                "Missing label for ${pin.packageName}",
                SystemAppPlaceholders.label(pin.packageName),
            )
        }
    }

    @Test
    fun unknownPackage_hasNoPlaceholder() {
        assertTrue(SystemAppPlaceholders.iconResId("com.example.unknown") == null)
    }
}
