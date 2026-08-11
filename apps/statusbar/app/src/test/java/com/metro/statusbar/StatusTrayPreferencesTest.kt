package com.metro.statusbar

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StatusTrayPreferencesTest {
    private lateinit var prefs: StatusTrayPreferences

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("metro_statusbar", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = StatusTrayPreferences(context)
    }

    @Test
    fun enabled_defaultsToFalse() {
        assertFalse(prefs.enabled)
    }

    @Test
    fun enabled_persistsAcrossInstances() {
        prefs.enabled = true
        val again = StatusTrayPreferences(RuntimeEnvironment.getApplication())
        assertTrue(again.enabled)
        again.enabled = false
        assertFalse(StatusTrayPreferences(RuntimeEnvironment.getApplication()).enabled)
    }

    @Test
    fun iconHideTimeout_defaultsToFiveSeconds() {
        assertEquals(StatusTrayPreferences.TIMEOUT_5S_MS, prefs.iconHideTimeoutMs)
        assertEquals(5_000L, StatusTrayPreferences.DEFAULT_ICON_HIDE_TIMEOUT_MS)
    }

    @Test
    fun iconHideTimeout_persistsAllowedValues() {
        prefs.iconHideTimeoutMs = StatusTrayPreferences.TIMEOUT_3S_MS
        assertEquals(
            StatusTrayPreferences.TIMEOUT_3S_MS,
            StatusTrayPreferences(RuntimeEnvironment.getApplication()).iconHideTimeoutMs,
        )
        prefs.iconHideTimeoutMs = StatusTrayPreferences.TIMEOUT_10S_MS
        assertEquals(
            StatusTrayPreferences.TIMEOUT_10S_MS,
            StatusTrayPreferences(RuntimeEnvironment.getApplication()).iconHideTimeoutMs,
        )
    }

    @Test
    fun iconHideTimeout_coercesUnknownValuesToDefault() {
        assertEquals(
            StatusTrayPreferences.DEFAULT_ICON_HIDE_TIMEOUT_MS,
            StatusTrayPreferences.coerceIconHideTimeoutMs(7_000L),
        )
        prefs.iconHideTimeoutMs = 1_000L
        assertEquals(StatusTrayPreferences.DEFAULT_ICON_HIDE_TIMEOUT_MS, prefs.iconHideTimeoutMs)
    }
}
