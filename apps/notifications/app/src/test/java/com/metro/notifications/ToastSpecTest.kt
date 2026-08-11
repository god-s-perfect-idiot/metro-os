package com.metro.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class ToastSpecTest {
    @Test
    fun coerceDurationMs_keepsAllowedValues() {
        assertEquals(ToastSpec.DURATION_3S_MS, ToastSpec.coerceDurationMs(3_000L))
        assertEquals(ToastSpec.DURATION_5S_MS, ToastSpec.coerceDurationMs(5_000L))
        assertEquals(ToastSpec.DURATION_10S_MS, ToastSpec.coerceDurationMs(10_000L))
    }

    @Test
    fun coerceDurationMs_fallsBackToFiveSeconds() {
        assertEquals(ToastSpec.DURATION_MS, ToastSpec.coerceDurationMs(0L))
        assertEquals(ToastSpec.DURATION_MS, ToastSpec.coerceDurationMs(7_000L))
    }

    @Test
    fun flipCameraInches_scalesWithBannerWidth() {
        assertEquals(13.5f, ToastSpec.flipCameraInches(1080f), 0.01f)
        assertEquals(9f, ToastSpec.flipCameraInches(720f), 0.01f)
    }

    @Test
    fun flipCameraInches_defaultsWhenUnmeasured() {
        assertEquals(8f, ToastSpec.flipCameraInches(0f), 0f)
        assertEquals(8f, ToastSpec.flipCameraInches(-1f), 0f)
    }
}
