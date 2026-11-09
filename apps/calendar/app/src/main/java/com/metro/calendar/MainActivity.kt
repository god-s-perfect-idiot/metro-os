package com.metro.calendar

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.calendar.tiles.CalendarTileRefresh
import com.metro.calendar.ui.CalendarShell
import com.metro.calendar.ui.CalendarState
import com.metro.calendar.ui.PermissionScreen
import com.metro.ui.MetroActivities
import com.metro.ui.MetroSplash
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroSystemTheme

class MainActivity : ComponentActivity() {
    private val requestCalendar = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results[Manifest.permission.READ_CALENDAR] == true
        permissionResult?.invoke(granted)
        // WRITE may arrive with the same grant dialog when both were requested.
        writePermissionResult?.invoke(results[Manifest.permission.WRITE_CALENDAR] == true)
    }

    private val requestWriteCalendar = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        writePermissionResult?.invoke(granted)
    }

    private var permissionResult: ((Boolean) -> Unit)? = null
    private var writePermissionResult: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        MetroSplash.install(this)
        super.onCreate(savedInstanceState)
        MetroActivities.applyLaunchTransition(this)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val state = remember { CalendarState(context) }
            var permissionTick by remember { mutableStateOf(0) }
            val generation = state.generation
            @Suppress("UNUSED_VARIABLE")
            val observeCalendarState = generation

            DisposableEffect(this@MainActivity) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        permissionTick++
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            DisposableEffect(permissionTick) {
                state.refreshPermission(context)
                if (state.hasCalendarPermission || state.skippedPermissions) {
                    state.reloadEvents()
                }
                CalendarTileRefresh.request(context)
                onDispose { }
            }

            MetroSystemTheme {
                MetroAppPivotShell(
                    modifier = Modifier.fillMaxSize(),
                    onExit = { MetroActivities.finishWithExitTransition(this@MainActivity) },
                ) {
                    when {
                        !state.permissionsChecked -> {
                            MetroLoadingScreen(modifier = Modifier.fillMaxSize())
                        }
                        state.needsPermissionGate -> {
                            PermissionScreen(
                                onRequestPermission = {
                                    permissionResult = { granted ->
                                        state.onPermissionResult(granted)
                                    }
                                    writePermissionResult = { granted ->
                                        state.onWritePermissionResult(granted)
                                    }
                                    requestCalendar.launch(
                                        arrayOf(
                                            Manifest.permission.READ_CALENDAR,
                                            Manifest.permission.WRITE_CALENDAR,
                                        ),
                                    )
                                },
                                onContinueWithDemo = state::continueWithDemo,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        else -> {
                            CalendarShell(
                                state = state,
                                onRequestWritePermission = {
                                    writePermissionResult = { granted ->
                                        state.onWritePermissionResult(granted)
                                    }
                                    requestWriteCalendar.launch(Manifest.permission.WRITE_CALENDAR)
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}
