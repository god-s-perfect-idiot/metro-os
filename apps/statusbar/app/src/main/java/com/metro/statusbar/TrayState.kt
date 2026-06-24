package com.metro.statusbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroPreferences
import java.time.ZonedDateTime

class TrayState(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = MetroPreferences(appContext)

    var expanded by mutableStateOf(false)
        private set

    var showProgress by mutableStateOf(false)
        private set

    var visibilityMode by mutableStateOf(TrayVisibilityMode.Opaque)
        private set

    var theme by mutableStateOf(TrayThemeResolver.resolve(preferences, visibilityMode))
        private set

    var clockText by mutableStateOf(TrayClockFormatter.format())
        private set

    var lastExpandedAtMs by mutableLongStateOf(0L)
        private set

    val snapshot: TraySnapshot
        get() = TraySnapshot(
            clockText = clockText,
            expanded = expanded,
            showProgress = showProgress,
            indicators = if (expanded) TrayIndicatorOrder.default else emptyList(),
            theme = theme,
        )

    private val themeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MetroBroadcasts.ACTION_THEME_CHANGED) {
                refreshTheme()
            }
        }
    }

    fun refreshTheme() {
        theme = TrayThemeResolver.resolve(preferences, visibilityMode)
    }

    fun refreshClock(now: ZonedDateTime = ZonedDateTime.now()) {
        clockText = TrayClockFormatter.format(now)
    }

    fun expand(nowMs: Long = System.currentTimeMillis()) {
        expanded = true
        lastExpandedAtMs = nowMs
    }

    fun collapse() {
        expanded = false
    }

    fun toggleExpanded(nowMs: Long = System.currentTimeMillis()) {
        if (expanded) {
            collapse()
        } else {
            expand(nowMs)
        }
    }

    fun tickAutoCollapse(nowMs: Long = System.currentTimeMillis()) {
        if (TrayCollapseScheduler.shouldAutoCollapse(expanded, lastExpandedAtMs, nowMs)) {
            collapse()
        }
    }

    fun setProgressVisible(visible: Boolean) {
        showProgress = visible
    }

    fun applyVisibilityMode(mode: TrayVisibilityMode) {
        visibilityMode = mode
        refreshTheme()
    }

    fun registerReceivers(context: Context) {
        val filter = IntentFilter(MetroBroadcasts.ACTION_THEME_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(themeReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(themeReceiver, filter)
        }
    }

    fun unregisterReceivers(context: Context) {
        runCatching { context.unregisterReceiver(themeReceiver) }
    }
}
