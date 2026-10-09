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
        prefs.iconHideTimeoutMs = StatusTrayPreferences.TIMEOUT_NEVER_MS
        assertEquals(
            StatusTrayPreferences.TIMEOUT_NEVER_MS,
            StatusTrayPreferences(RuntimeEnvironment.getApplication()).iconHideTimeoutMs,
        )
        assertTrue(StatusTrayPreferences(RuntimeEnvironment.getApplication()).neverHidesIcons)
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

    @Test
    fun notchPosition_defaultsToCenter() {
        assertEquals(NotchPosition.Center, prefs.notchPosition)
    }

    @Test
    fun notchPosition_persistsAcrossInstances() {
        prefs.notchPosition = NotchPosition.Left
        assertEquals(
            NotchPosition.Left,
            StatusTrayPreferences(RuntimeEnvironment.getApplication()).notchPosition,
        )
        prefs.notchPosition = NotchPosition.Right
        assertEquals(
            NotchPosition.Right,
            StatusTrayPreferences(RuntimeEnvironment.getApplication()).notchPosition,
        )
        prefs.notchPosition = NotchPosition.Center
        assertEquals(
            NotchPosition.Center,
            StatusTrayPreferences(RuntimeEnvironment.getApplication()).notchPosition,
        )
    }

    @Test
    fun notchPosition_unknownStorageFallsBackToCenter() {
        assertEquals(NotchPosition.Center, NotchPosition.fromStorage(null))
        assertEquals(NotchPosition.Center, NotchPosition.fromStorage("top"))
        assertEquals(NotchPosition.Left, NotchPosition.fromStorage(NotchPosition.STORAGE_LEFT))
        assertEquals(NotchPosition.Right, NotchPosition.fromStorage(NotchPosition.STORAGE_RIGHT))
    }

    @Test
    fun backgroundMode_defaultsToDefaultBlackBackground() {
        assertEquals(StatusBarBackgroundMode.DefaultBlackBackground, prefs.backgroundMode)
    }

    @Test
    fun backgroundMode_persistsAcrossInstances() {
        prefs.backgroundMode = StatusBarBackgroundMode.MatchAppBackground
        assertEquals(
            StatusBarBackgroundMode.MatchAppBackground,
            StatusTrayPreferences(RuntimeEnvironment.getApplication()).backgroundMode,
        )
        prefs.backgroundMode = StatusBarBackgroundMode.ShowAccentColor
        assertEquals(
            StatusBarBackgroundMode.ShowAccentColor,
            StatusTrayPreferences(RuntimeEnvironment.getApplication()).backgroundMode,
        )
        prefs.backgroundMode = StatusBarBackgroundMode.DefaultBlackBackground
        assertEquals(
            StatusBarBackgroundMode.DefaultBlackBackground,
            StatusTrayPreferences(RuntimeEnvironment.getApplication()).backgroundMode,
        )
    }

    @Test
    fun backgroundMode_unknownStorageFallsBackToDefaultBlack() {
        assertEquals(
            StatusBarBackgroundMode.DefaultBlackBackground,
            StatusBarBackgroundMode.fromStorage(null),
        )
        assertEquals(
            StatusBarBackgroundMode.DefaultBlackBackground,
            StatusBarBackgroundMode.fromStorage("theme"),
        )
        assertEquals(
            StatusBarBackgroundMode.MatchAppBackground,
            StatusBarBackgroundMode.fromStorage(StatusBarBackgroundMode.STORAGE_MATCH_APP),
        )
        assertEquals(
            StatusBarBackgroundMode.ShowAccentColor,
            StatusBarBackgroundMode.fromStorage(StatusBarBackgroundMode.STORAGE_SHOW_ACCENT),
        )
        assertEquals(
            StatusBarBackgroundMode.DefaultBlackBackground,
            StatusBarBackgroundMode.fromStorage(StatusBarBackgroundMode.STORAGE_DEFAULT_BLACK),
        )
    }

    @Test
    fun backgroundMode_migratesLegacyMatchBoolean() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("metro_statusbar", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putBoolean("match_app_background", true)
            .commit()
        assertEquals(
            StatusBarBackgroundMode.MatchAppBackground,
            StatusTrayPreferences(context).backgroundMode,
        )
        context.getSharedPreferences("metro_statusbar", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putBoolean("match_app_background", false)
            .commit()
        assertEquals(
            StatusBarBackgroundMode.DefaultBlackBackground,
            StatusTrayPreferences(context).backgroundMode,
        )
    }

    @Test
    fun iconFlags_defaultAllOn() {
        val flags = prefs.iconFlags
        assertTrue(flags.network)
        assertTrue(flags.wifi)
        assertTrue(flags.mute)
        assertTrue(flags.notifications)
        assertTrue(flags.hotspot)
        assertTrue(flags.bluetoothAudio)
        assertTrue(flags.battery)
    }

    @Test
    fun iconFlags_persistAcrossInstances() {
        prefs.iconFlags = TrayIconFlags(
            network = true,
            wifi = false,
            mute = true,
            notifications = false,
            hotspot = true,
            bluetoothAudio = false,
            battery = true,
        )
        val again = StatusTrayPreferences(RuntimeEnvironment.getApplication()).iconFlags
        assertTrue(again.network)
        assertFalse(again.wifi)
        assertTrue(again.mute)
        assertFalse(again.notifications)
        assertTrue(again.hotspot)
        assertFalse(again.bluetoothAudio)
        assertTrue(again.battery)
    }

    @Test
    fun layout_defaultsToWpOrder() {
        assertEquals(TrayLayout.DEFAULT, prefs.layout)
    }

    @Test
    fun layout_persistsAcrossInstances() {
        val slots = TrayLayout.addSpacer(
            TrayLayout.move(TrayLayout.DEFAULT, fromIndex = 0, toIndex = 3),
            TrayIconFlags(),
        )
        prefs.layout = slots
        val again = StatusTrayPreferences(RuntimeEnvironment.getApplication()).layout
        assertEquals(TrayLayout.serialize(slots), TrayLayout.serialize(again))
    }

}
