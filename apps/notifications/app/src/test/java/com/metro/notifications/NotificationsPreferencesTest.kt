package com.metro.notifications

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
class NotificationsPreferencesTest {
    private lateinit var prefs: NotificationsPreferences

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("metro_notifications", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = NotificationsPreferences(context)
    }

    @Test
    fun enabled_defaultsToFalse() {
        assertFalse(prefs.enabled)
    }

    @Test
    fun enabled_persistsAcrossInstances() {
        prefs.enabled = true
        val again = NotificationsPreferences(RuntimeEnvironment.getApplication())
        assertTrue(again.enabled)
        again.enabled = false
        assertFalse(NotificationsPreferences(RuntimeEnvironment.getApplication()).enabled)
    }

    @Test
    fun toastDurationMs_defaultsToFiveSeconds() {
        assertEquals(ToastSpec.DURATION_MS, prefs.toastDurationMs)
    }

    @Test
    fun toastDurationMs_persistsAllowedValues() {
        prefs.toastDurationMs = ToastSpec.DURATION_3S_MS
        assertEquals(
            ToastSpec.DURATION_3S_MS,
            NotificationsPreferences(RuntimeEnvironment.getApplication()).toastDurationMs,
        )
        prefs.toastDurationMs = ToastSpec.DURATION_10S_MS
        assertEquals(
            ToastSpec.DURATION_10S_MS,
            NotificationsPreferences(RuntimeEnvironment.getApplication()).toastDurationMs,
        )
    }

    @Test
    fun toastDurationMs_rejectsUnknownValues() {
        prefs.toastDurationMs = 7_000L
        assertEquals(ToastSpec.DURATION_MS, prefs.toastDurationMs)
    }
}
