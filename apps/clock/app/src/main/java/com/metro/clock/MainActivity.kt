package com.metro.clock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.metro.clock.data.AlarmItem
import com.metro.clock.data.AlarmNotifier
import com.metro.clock.data.AlarmScheduler
import com.metro.clock.data.AlarmStore
import com.metro.clock.data.ClockAlert
import com.metro.clock.data.TimerScheduler
import com.metro.clock.data.WorldClockEntry
import com.metro.clock.data.WorldClockStore
import com.metro.clock.ui.ClockShell
import com.metro.ui.MetroActivities
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroSplash
import com.metro.ui.MetroSystemTheme

class MainActivity : ComponentActivity() {
    private lateinit var alarmStore: AlarmStore
    private lateinit var worldStore: WorldClockStore
    private var alarms by mutableStateOf<List<AlarmItem>>(emptyList())
    private var worldCities by mutableStateOf<List<WorldClockEntry>>(emptyList())
    private var pendingAlert by mutableStateOf<ClockAlert?>(null)

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* channel still created; notifications improve background delivery */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        MetroSplash.install(this)
        super.onCreate(savedInstanceState)
        MetroActivities.applyLaunchTransition(this)
        enableEdgeToEdge()
        alarmStore = AlarmStore(this)
        worldStore = WorldClockStore(this)
        alarms = alarmStore.load()
        worldCities = worldStore.load()
        AlarmNotifier.ensureChannel(this)
        AlarmScheduler.rescheduleAll(this, alarms)
        requestNotificationPermissionIfNeeded()
        consumeAlertIntent(intent)

        setContent {
            MetroSystemTheme {
                MetroAppPivotShell(
                    modifier = Modifier.fillMaxSize(),
                    onExit = { MetroActivities.finishWithExitTransition(this@MainActivity) },
                ) {
                    ClockShell(
                        alarms = alarms,
                        worldCities = worldCities,
                        onSaveAlarm = { alarm ->
                            alarms = alarmStore.upsert(alarm)
                            AlarmScheduler.schedule(this@MainActivity, alarm)
                        },
                        onDeleteAlarm = { id ->
                            AlarmScheduler.cancel(this@MainActivity, id)
                            alarms = alarmStore.delete(id)
                        },
                        onToggleAlarm = { id, enabled ->
                            alarms = alarmStore.setEnabled(id, enabled)
                            val updated = alarms.firstOrNull { it.id == id }
                            if (updated != null) {
                                AlarmScheduler.schedule(this@MainActivity, updated)
                            } else {
                                AlarmScheduler.cancel(this@MainActivity, id)
                            }
                        },
                        onAddWorldCity = { id ->
                            worldCities = worldStore.add(id)
                        },
                        onDeleteWorldCities = { ids ->
                            worldCities = worldStore.remove(ids)
                        },
                        pendingAlert = pendingAlert,
                        onPendingAlertConsumed = { pendingAlert = null },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeAlertIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        alarms = alarmStore.load()
    }

    private fun consumeAlertIntent(intent: Intent?) {
        if (intent == null) return
        when {
            intent.getStringExtra(TimerScheduler.EXTRA_RING_KIND) == TimerScheduler.RING_KIND_TIMER -> {
                pendingAlert = ClockAlert.Timer
                intent.removeExtra(TimerScheduler.EXTRA_RING_KIND)
            }
            else -> {
                val id = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
                val alarm = alarmStore.load().firstOrNull { it.id == id } ?: return
                pendingAlert = ClockAlert.Alarm(alarm)
                intent.removeExtra(AlarmScheduler.EXTRA_ALARM_ID)
                AlarmNotifier.cancelAlarm(this, id)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
