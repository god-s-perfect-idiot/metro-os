package com.metro.notifications

import android.content.Context
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class HeadsUpControllerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("metro_notifications", Context.MODE_PRIVATE).edit().clear().commit()
        Settings.Global.putInt(
            context.contentResolver,
            HeadsUpController.HEADS_UP_NOTIFICATIONS_ENABLED,
            1,
        )
    }

    @Test
    fun disableStockHeadsUp_setsGlobalToZero() {
        HeadsUpController.disableStockHeadsUp(context)
        assertEquals(
            0,
            Settings.Global.getInt(
                context.contentResolver,
                HeadsUpController.HEADS_UP_NOTIFICATIONS_ENABLED,
                1,
            ),
        )
        assertTrue(HeadsUpController.isStockHeadsUpDisabled(context))
    }

    @Test
    fun disableStockHeadsUp_preservesPreviousAcrossReentrantCalls() {
        HeadsUpController.disableStockHeadsUp(context)
        HeadsUpController.disableStockHeadsUp(context)
        assertEquals(1, NotificationsPreferences(context).previousHeadsUpEnabled)
    }

    @Test
    fun restoreStockHeadsUp_noOpsWhileMasterToggleEnabled() {
        val prefs = NotificationsPreferences(context)
        prefs.enabled = true
        HeadsUpController.disableStockHeadsUp(context)
        HeadsUpController.restoreStockHeadsUp(context)
        assertEquals(
            0,
            Settings.Global.getInt(
                context.contentResolver,
                HeadsUpController.HEADS_UP_NOTIFICATIONS_ENABLED,
                1,
            ),
        )
    }

    @Test
    fun restoreStockHeadsUp_restoresPreviousWhenDisabled() {
        HeadsUpController.disableStockHeadsUp(context)
        NotificationsPreferences(context).enabled = false
        HeadsUpController.restoreStockHeadsUp(context)
        assertEquals(
            1,
            Settings.Global.getInt(
                context.contentResolver,
                HeadsUpController.HEADS_UP_NOTIFICATIONS_ENABLED,
                0,
            ),
        )
    }
}
