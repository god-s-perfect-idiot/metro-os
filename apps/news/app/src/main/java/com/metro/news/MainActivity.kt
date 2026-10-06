package com.metro.news

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.metro.news.ui.NewsShell
import com.metro.news.ui.NewsState
import com.metro.ui.MetroActivities
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroSplash
import com.metro.ui.MetroSystemTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        MetroSplash.install(this)
        super.onCreate(savedInstanceState)
        MetroActivities.applyLaunchTransition(this)
        enableEdgeToEdge()
        setContent {
            MetroSystemTheme {
                MetroAppPivotShell(
                    modifier = Modifier.fillMaxSize(),
                    onExit = { MetroActivities.finishWithExitTransition(this@MainActivity) },
                ) {
                    val state = remember { NewsState(this@MainActivity) }
                    NewsShell(
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
