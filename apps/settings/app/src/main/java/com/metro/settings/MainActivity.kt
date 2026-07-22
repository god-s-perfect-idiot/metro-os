package com.metro.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.metro.settings.ui.SettingsShell
import com.metro.settings.ui.SettingsState
import com.metro.ui.MetroSystemTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val state = remember { SettingsState(context) }
            MetroSystemTheme {
                SettingsShell(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
