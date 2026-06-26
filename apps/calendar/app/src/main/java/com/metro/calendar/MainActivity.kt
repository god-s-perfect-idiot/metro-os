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
import com.metro.ui.MetroTheme

class MainActivity : ComponentActivity() {
    private val requestCalendar = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionResult?.invoke(granted)
    }

    private var permissionResult: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

            MetroTheme {
                if (state.needsPermissionGate) {
                    PermissionScreen(
                        onRequestPermission = {
                            permissionResult = { granted ->
                                state.onPermissionResult(granted)
                            }
                            requestCalendar.launch(Manifest.permission.READ_CALENDAR)
                        },
                        onContinueWithDemo = state::continueWithDemo,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    CalendarShell(
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
