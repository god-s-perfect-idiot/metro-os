package com.metro.volume

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class VolumeHudPreferencesTest {
    private lateinit var prefs: VolumeHudPreferences

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("metro_volume", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = VolumeHudPreferences(context)
    }

    @Test
    fun enabled_defaultsToFalse() {
        assertFalse(prefs.enabled)
    }

    @Test
    fun enabled_persistsAcrossInstances() {
        prefs.enabled = true
        val again = VolumeHudPreferences(RuntimeEnvironment.getApplication())
        assertTrue(again.enabled)
        again.enabled = false
        assertFalse(VolumeHudPreferences(RuntimeEnvironment.getApplication()).enabled)
    }
}
