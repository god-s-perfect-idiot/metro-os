package com.metro.keyboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.metro.keyboard.ui.KeyboardSettingsShell
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroSystemTheme
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.setup.NotificationPermissionState
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.jetpref.datastore.model.collectAsState as collectPrefAsState
import kotlinx.coroutines.launch
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.compose.ProvideLocalizedResources

class MainActivity : ComponentActivity() {
    private val appContext by appContext()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().apply {
            setKeepOnScreenCondition { !appContext.preferenceStoreLoaded.value }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefsLoaded by appContext.preferenceStoreLoaded.collectAsState()
            ProvideLocalizedResources(
                this@MainActivity,
                appName = R.string.app_name,
            ) {
                MetroSystemTheme {
                    MetroAppPivotShell(
                        modifier = Modifier.fillMaxSize(),
                        onExit = { finish() },
                    ) {
                        KeyboardSettingsApp(
                            prefsLoaded = prefsLoaded,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardSettingsApp(
    prefsLoaded: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!prefsLoaded) {
        Box(modifier = modifier)
        return
    }

    val prefs by FlorisPreferenceStore
    val notificationPermissionState by prefs.internal.notificationPermissionState.collectPrefAsState()
    val scope = rememberCoroutineScope()
    val requestNotificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            scope.launch {
                prefs.internal.notificationPermissionState.set(
                    if (granted) NotificationPermissionState.GRANTED else NotificationPermissionState.DENIED,
                )
            }
        }

    KeyboardSettingsShell(
        modifier = modifier,
        notificationPermissionState = notificationPermissionState,
        onRequestNotification = if (AndroidVersion.ATLEAST_API33_T) {
            { requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
        } else {
            null
        },
    )
}
