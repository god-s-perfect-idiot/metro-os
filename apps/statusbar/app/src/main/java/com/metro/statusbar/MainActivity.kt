package com.metro.statusbar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.statusbar.ui.StatusTray
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val state = remember { TrayState(context) }
            var permissionTick by remember { mutableIntStateOf(0) }

            DisposableEffect(this@MainActivity) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        permissionTick++
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            DisposableEffect(state) {
                state.registerReceivers(context)
                state.refreshTheme()
                state.refreshClock()
                onDispose { state.unregisterReceivers(context) }
            }

            val overlayGranted = remember(permissionTick) { Settings.canDrawOverlays(context) }

            MetroTheme(
                darkTheme = state.theme.darkTheme,
                accent = state.theme.accentColor,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Top,
                ) {
                    MetroPageHeader(title = stringResource(R.string.app_name))
                    MetroText(
                        text = stringResource(R.string.permission_overlay_body),
                        style = MetroTextStyle.Body,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    ShellActionButton(
                        label = stringResource(R.string.grant_overlay),
                        enabled = !overlayGranted,
                        onClick = {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName"),
                                ),
                            )
                        },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ShellActionButton(
                        label = stringResource(R.string.start_overlay),
                        enabled = overlayGranted,
                        onClick = { StatusBarOverlayService.start(context) },
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    MetroText(
                        text = "Preview",
                        style = MetroTextStyle.SectionHeader,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    StatusTray(
                        snapshot = state.snapshot,
                        onTrayTap = { state.toggleExpanded() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ShellActionButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier
                        .padding(vertical = 8.dp)
                        .clickable(onClick = onClick)
                } else {
                    Modifier.padding(vertical = 8.dp)
                },
            ),
        horizontalAlignment = Alignment.Start,
    ) {
        MetroText(
            text = label,
            style = MetroTextStyle.ListItemTitle,
            color = if (enabled) MetroTheme.colors.accent else MetroTheme.colors.secondaryText,
        )
    }
}
