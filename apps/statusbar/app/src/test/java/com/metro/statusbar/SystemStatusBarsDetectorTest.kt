package com.metro.statusbar

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemStatusBarsDetectorTest {
    @Test
    fun areHiddenFromVisible_nullOrTrue_isNotHidden() {
        assertFalse(SystemStatusBarsDetector.areHiddenFromVisible(null))
        assertFalse(SystemStatusBarsDetector.areHiddenFromVisible(true))
    }

    @Test
    fun areHiddenFromVisible_false_isHidden() {
        assertTrue(SystemStatusBarsDetector.areHiddenFromVisible(false))
    }
}
