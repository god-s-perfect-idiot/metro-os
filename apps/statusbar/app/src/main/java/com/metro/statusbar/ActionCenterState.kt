package com.metro.statusbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Action Center shade state: open fraction, quick actions, and notification groups.
 * Owned by [StatusBarOverlayService] alongside [TrayState].
 */
class ActionCenterState(context: Context) {
    private val appContext = context.applicationContext
    private val quickActions = QuickActionController(appContext)

    /** 0 = closed, 1 = fully open. Drives height + slide animation. */
    var openFraction by mutableFloatStateOf(0f)
        private set

    var fullyOpen by mutableStateOf(false)
        private set

    var slots by mutableStateOf(quickActions.snapshot())
        private set

    var notificationGroups by mutableStateOf(ActionNotificationStore.all())
        private set

    var dateText by mutableStateOf(ActionCenterDateFormatter.format())
        private set

    private val notificationListener: () -> Unit = {
        notificationGroups = ActionNotificationStore.all()
    }

    private val radioReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshQuickActions()
        }
    }

    val isOpenOrOpening: Boolean
        get() = openFraction > 0.01f

    fun refreshQuickActions() {
        slots = quickActions.snapshot()
    }

    fun refreshDate(now: ZonedDateTime = ZonedDateTime.now()) {
        dateText = ActionCenterDateFormatter.format(now)
    }

    fun refreshNotifications() {
        notificationGroups = ActionNotificationStore.all()
    }

    fun updateOpenFraction(fraction: Float) {
        openFraction = fraction.coerceIn(0f, 1f)
        fullyOpen = openFraction >= 0.99f
    }

    fun open() {
        updateOpenFraction(1f)
        refreshQuickActions()
        refreshNotifications()
    }

    fun close() {
        updateOpenFraction(0f)
    }

    fun toggleQuickAction(type: QuickActionType) {
        quickActions.toggle(type)
        refreshQuickActions()
    }

    fun clearAllNotifications() {
        ActionNotificationListenerService.clearAll()
        notificationGroups = emptyList()
    }

    fun openAllSettings() {
        val launch = appContext.packageManager.getLaunchIntentForPackage("com.metro.settings")
            ?: Intent(android.provider.Settings.ACTION_SETTINGS)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(launch)
        close()
    }

    fun openNotification(item: ActionNotificationItem) {
        ActionNotificationListenerService.openNotification(item.key)
        close()
    }

    fun register(context: Context) {
        ActionNotificationStore.addListener(notificationListener)
        refreshNotifications()
        refreshQuickActions()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction("android.net.wifi.WIFI_STATE_CHANGED")
            addAction("android.net.wifi.STATE_CHANGE")
            addAction("android.bluetooth.adapter.action.STATE_CHANGED")
            addAction("android.net.conn.TETHER_STATE_CHANGED")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(radioReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(radioReceiver, filter)
        }
    }

    fun unregister(context: Context) {
        ActionNotificationStore.removeListener(notificationListener)
        runCatching { context.unregisterReceiver(radioReceiver) }
    }
}

object ActionCenterDateFormatter {
    private val formatter = DateTimeFormatter.ofPattern("M/d", Locale.getDefault())

    fun format(now: ZonedDateTime = ZonedDateTime.now()): String = formatter.format(now)
}
