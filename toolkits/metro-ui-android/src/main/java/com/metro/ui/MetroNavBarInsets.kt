package com.metro.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroNavBar
import com.metro.system.MetroPreferences

/**
 * Observes whether the Metro navigation bar overlay (`com.metro.navbar`) is currently enabled.
 *
 * The navbar is a separate overlay window, so apps never receive it as a window inset. This helper
 * keeps a reactive flag in sync by:
 *  1. seeding from the last value cached in [MetroPreferences],
 *  2. listening for live [MetroBroadcasts.ACTION_NAVBAR_CHANGED] updates, and
 *  3. asking the navbar for its current state on first composition (cold-start correctness).
 *
 * Prefer [metroNavBarPadding] over reading this directly.
 */
@Composable
fun rememberMetroNavBarEnabled(): State<Boolean> {
    val context = LocalContext.current
    val enabled = remember(context) {
        mutableStateOf(MetroPreferences(context).navBarEnabled)
    }

    DisposableEffect(context) {
        val appContext = context.applicationContext
        val prefs = MetroPreferences(appContext)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(received: Context?, intent: Intent?) {
                if (intent?.action != MetroBroadcasts.ACTION_NAVBAR_CHANGED) return
                val value = intent.getBooleanExtra(MetroBroadcasts.EXTRA_NAVBAR_ENABLED, false)
                enabled.value = value
                prefs.navBarEnabled = value
            }
        }

        val filter = IntentFilter(MetroBroadcasts.ACTION_NAVBAR_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            appContext.registerReceiver(receiver, filter)
        }

        appContext.sendBroadcast(
            Intent(MetroBroadcasts.ACTION_NAVBAR_QUERY).setPackage(MetroNavBar.PACKAGE),
        )

        onDispose { runCatching { appContext.unregisterReceiver(receiver) } }
    }

    return enabled
}

/**
 * The standard way for every Metro app to respect the navigation bar.
 *
 * Adds [MetroNavBar.HEIGHT_DP] of bottom padding while the navbar overlay is enabled, so app
 * content (keypads, app bars, lists, action buttons) never sits underneath the soft keys. When the
 * navbar is disabled this is a no-op and the app keeps the full screen height.
 *
 * Apply it to the root container of every screen, after any [statusBarsPadding]/
 * [navigationBarsPadding] and before the background so the reserved strip stays transparent:
 *
 * ```
 * Column(
 *     modifier = Modifier
 *         .fillMaxSize()
 *         .statusBarsPadding()
 *         .metroNavBarPadding()
 *         .background(MetroTheme.colors.background),
 * ) { /* … */ }
 * ```
 */
@Composable
fun Modifier.metroNavBarPadding(): Modifier {
    val enabled by rememberMetroNavBarEnabled()
    return if (enabled) this.padding(bottom = MetroNavBar.HEIGHT_DP.dp) else this
}
