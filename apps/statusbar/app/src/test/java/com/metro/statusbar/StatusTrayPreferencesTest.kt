package com.metro.statusbar

import android.content.Context
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
}
