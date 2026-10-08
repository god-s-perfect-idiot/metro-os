package com.metro.camera

import com.metro.camera.ui.CameraUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraUiStateTest {
    @Test
    fun defaultSlots_areFourUnderGallery() {
        // Gallery is fixed; remaining left shortcuts ≤ 4 (flash, flip, lens, auto).
        assertTrue(CameraUiState.QuickSetting.defaults.size in 1..4)
        assertEquals(
            listOf(
                CameraUiState.QuickSetting.Flash,
                CameraUiState.QuickSetting.SwitchCamera,
                CameraUiState.QuickSetting.Lenses,
                CameraUiState.QuickSetting.Iso,
            ),
            CameraUiState.QuickSetting.defaults,
        )
    }

    @Test
    fun defaultMode_isPhoto() {
        assertEquals(CameraUiState.Mode.Photo, CameraUiState().mode)
    }

    @Test
    fun burstRetention_defaultsToSevenDays() {
        assertEquals(7, CameraUiState().burstRetentionDays)
    }
}
