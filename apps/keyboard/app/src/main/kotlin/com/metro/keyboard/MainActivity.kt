package com.metro.keyboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.metro.keyboard.ui.KeyboardSettingsShell
import com.metro.ui.MetroSystemTheme

/**
 * WP8.1-style keyboard settings entry point (Metro UX).
 * The IME service remains FlorisBoard-derived under [dev.patrickgold.florisboard].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MetroSystemTheme {
                KeyboardSettingsShell(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
