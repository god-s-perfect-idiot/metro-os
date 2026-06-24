package com.metro.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
                onDispose { state.unregisterReceivers(context) }
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
