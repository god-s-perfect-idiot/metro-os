package com.metro.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.launcher.ui.LauncherShell
import com.metro.launcher.ui.LauncherState
import com.metro.ui.MetroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val state = remember { LauncherState(context) }
            DisposableEffect(state) {
                state.registerReceivers(context)
                state.refreshAll()
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        state.refreshAll()
                    }
                }
                lifecycle.addObserver(observer)
                onDispose {
                    lifecycle.removeObserver(observer)
                    state.unregisterReceivers(context)
                }
            }
            MetroTheme(
                darkTheme = state.darkTheme,
                accent = state.accent,
            ) {
                LauncherShell(state = state)
            }
        }
    }
}
