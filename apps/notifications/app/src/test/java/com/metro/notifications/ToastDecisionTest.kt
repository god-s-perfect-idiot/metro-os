package com.metro.notifications

import android.app.Notification
import android.app.NotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToastDecisionTest {
    @Test
    fun highImportance_toastsWhenInteractive() {
        assertTrue(
            ToastDecision.shouldShow(
                packageName = "com.example.mail",
                flags = 0,
                importance = NotificationManager.IMPORTANCE_HIGH,
                matchesInterruptionFilter = true,
                screenInteractive = true,
                isGroupSummary = false,
                isActiveCall = false,
                onlyAlertOnceAlreadyShown = false,
            ),
        )
    }

    @Test
    fun defaultImportance_doesNotToast() {
        assertFalse(
            ToastDecision.shouldShow(
                packageName = "com.example.mail",
                flags = 0,
                importance = NotificationManager.IMPORTANCE_DEFAULT,
                matchesInterruptionFilter = true,
                screenInteractive = true,
                isGroupSummary = false,
                isActiveCall = false,
                onlyAlertOnceAlreadyShown = false,
            ),
        )
    }

    @Test
    fun screenOff_suppressesToast() {
        assertFalse(
            ToastDecision.shouldShow(
                packageName = "com.example.mail",
                flags = 0,
                importance = NotificationManager.IMPORTANCE_HIGH,
                matchesInterruptionFilter = true,
                screenInteractive = false,
                isGroupSummary = false,
                isActiveCall = false,
                onlyAlertOnceAlreadyShown = false,
            ),
        )
    }

    @Test
    fun ongoingAndShellPackages_neverToast() {
        assertFalse(
            ToastDecision.shouldShow(
                packageName = "com.example.mail",
                flags = Notification.FLAG_ONGOING_EVENT,
                importance = NotificationManager.IMPORTANCE_HIGH,
                matchesInterruptionFilter = true,
                screenInteractive = true,
                isGroupSummary = false,
                isActiveCall = false,
                onlyAlertOnceAlreadyShown = false,
            ),
        )
        assertFalse(
            ToastDecision.shouldShow(
                packageName = "com.metro.notifications",
                flags = 0,
                importance = NotificationManager.IMPORTANCE_HIGH,
                matchesInterruptionFilter = true,
                screenInteractive = true,
                isGroupSummary = false,
                isActiveCall = false,
                onlyAlertOnceAlreadyShown = false,
            ),
        )
    }

    @Test
    fun displayLine_joinsTitleAndBodyWithColon() {
        assertEquals(
            "Carl Cubillas also commented on your post",
            ToastSnapshot(
                key = "1",
                packageName = "com.example",
                title = "Carl Cubillas also commented on your post",
                body = null,
            ).displayLine(),
        )
        assertEquals(
            "Mom: Hello from Sam",
            ToastSnapshot(
                key = "2",
                packageName = "com.metro.messaging",
                title = "Mom",
                body = "Hello from Sam",
            ).displayLine(),
        )
        assertEquals(
            "Hello from Sam",
            ToastSnapshot(
                key = "3",
                packageName = "com.example",
                title = "Hello",
                body = "Hello from Sam",
            ).displayLine(),
        )
        assertEquals(
            "Alex: are you free later?",
            ToastSnapshot(
                key = "4",
                packageName = "com.whatsapp",
                title = "Alex",
                body = "are you free later?",
            ).displayLine(),
        )
    }
}
