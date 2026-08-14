package com.metro.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.metro.settings.ui.SettingsShell
import com.metro.settings.ui.SettingsState
import com.metro.ui.MetroSystemTheme

class MainActivity : ComponentActivity() {
    private val settingsState = mutableStateOf<SettingsState?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MetroActivities.applyLaunchTransition(this)
        enableEdgeToEdge()
        val state = SettingsState(this)
        settingsState.value = state
        setContent {
            MetroSystemTheme {
                SettingsShell(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        settingsState.value?.refreshSystemReads()
    }
}
