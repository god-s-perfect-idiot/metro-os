package com.metro.volume

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.system.MetroPreferences
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { MetroPreferences(context) }
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

            val overlayGranted = remember(permissionTick) { Settings.canDrawOverlays(context) }
            val accessibilityEnabled = remember(permissionTick) {
                VolumeAccessibilityService.isEnabled()
            }
            val accent = remember(permissionTick) { prefs.accentColor }

            MetroTheme(darkTheme = prefs.isDark, accent = accent) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .metroNavBarPadding(),
                    verticalArrangement = Arrangement.Top,
                ) {
                    MetroAppTitle(title = stringResource(R.string.app_name))
                    MetroText(
                        text = stringResource(R.string.setup_title),
                        style = MetroTextStyle.HubTitle,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                    )
                    MetroText(
                        text = stringResource(R.string.setup_body),
                        style = MetroTextStyle.DialogBody,
                        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 16.dp),
                    )

                    MetroBorderButton(
                        text = stringResource(R.string.grant_overlay),
                        enabled = !overlayGranted,
                        onClick = {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName"),
                                ),
                            )
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MetroBorderButton(
                        text = stringResource(R.string.enable_accessibility),
                        enabled = !accessibilityEnabled,
                        onClick = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    if (overlayGranted) {
                        // Keep the FGS warm so volume keys are never swallowed without a handler.
                        LaunchedEffect(overlayGranted, accessibilityEnabled) {
                            VolumeOverlayService.start(context)
                        }
                        MetroBorderButton(
                            text = stringResource(R.string.start_overlay),
                            onClick = { VolumeOverlayService.start(context) },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }

                    if (overlayGranted && accessibilityEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        BasicText(
                            text = stringResource(R.string.setup_ready),
                            modifier = Modifier.padding(horizontal = 12.dp),
                            style = TextStyle(
                                fontFamily = MetroFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                color = MetroTheme.colors.secondaryText,
                            ),
                        )
                    }
                }
            }
        }
    }
}
