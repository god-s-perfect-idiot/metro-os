package com.metro.hub.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.hub.R
import com.metro.hub.shizuku.ShizukuAvailability
import com.metro.hub.shizuku.ShizukuReadyState
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import rikka.shizuku.Shizuku

/**
 * Hub settings: master Shizuku-install toggle + grant / start / install Shizuku.
 * WP8.1 settings language (overline + page title + section + toggle).
 */
@Composable
fun SettingsScreen(
    state: HubState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observe = generation

    var shizukuTick by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val binderReceived = Shizuku.OnBinderReceivedListener { shizukuTick++ }
        val binderDead = Shizuku.OnBinderDeadListener { shizukuTick++ }
        val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, _ ->
            if (requestCode == ShizukuAvailability.PERMISSION_REQUEST_CODE) {
                shizukuTick++
                state.bumpShizukuState()
            }
        }
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        onDispose {
            Shizuku.removeBinderReceivedListener(binderReceived)
            Shizuku.removeBinderDeadListener(binderDead)
            Shizuku.removeRequestPermissionResultListener(permissionListener)
        }
    }

    val shizukuState = remember(shizukuTick, generation) {
        ShizukuAvailability.state(context)
    }
    val toggleEnabled = state.shizukuInstallEnabled
    val secondary = MetroTheme.colors.secondaryText

    Column(modifier = modifier.fillMaxSize()) {
        MetroAppTitle(title = stringResource(R.string.settings_overline))
        MetroText(
            text = stringResource(R.string.settings_title),
            style = MetroTextStyle.PageTitle,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 20.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            MetroText(
                text = stringResource(R.string.settings_install_section),
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp),
            )
            MetroText(
                text = stringResource(R.string.settings_install_body),
                style = MetroTextStyle.DialogBody,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 16.dp),
            )

            ShizukuSetupBlock(
                context = context,
                shizukuState = shizukuState,
                onStateChanged = {
                    shizukuTick++
                    state.bumpShizukuState()
                },
            )

            Spacer(modifier = Modifier.height(20.dp))

            MetroToggleSwitch(
                checked = toggleEnabled,
                onCheckedChange = state::applyShizukuInstallEnabled,
                enabled = true,
                label = stringResource(R.string.settings_shizuku_install_toggle),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            MetroText(
                text = if (toggleEnabled && shizukuState == ShizukuReadyState.Ready) {
                    stringResource(R.string.settings_shizuku_install_on_ready)
                } else if (toggleEnabled) {
                    stringResource(R.string.settings_shizuku_install_on_waiting)
                } else {
                    stringResource(R.string.settings_shizuku_install_off)
                },
                style = MetroTextStyle.DialogBody,
                color = secondary,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun ShizukuSetupBlock(
    context: Context,
    shizukuState: ShizukuReadyState,
    onStateChanged: () -> Unit,
) {
    when (shizukuState) {
        ShizukuReadyState.NotInstalled -> {
            MetroText(
                text = stringResource(R.string.settings_shizuku_nudge_not_installed),
                style = MetroTextStyle.DialogBody,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            )
            MetroBorderButton(
                text = stringResource(R.string.settings_shizuku_get),
                onClick = { ShizukuAvailability.openDownload(context) },
                modifier = Modifier.padding(horizontal = 12.dp),
                fontSize = 15.sp,
            )
        }
        ShizukuReadyState.NotRunning -> {
            MetroText(
                text = stringResource(R.string.settings_shizuku_nudge_not_running),
                style = MetroTextStyle.DialogBody,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            )
            MetroBorderButton(
                text = stringResource(R.string.settings_shizuku_open),
                onClick = {
                    ShizukuAvailability.openShizuku(context)
                    onStateChanged()
                },
                modifier = Modifier.padding(horizontal = 12.dp),
                fontSize = 15.sp,
            )
        }
        ShizukuReadyState.NeedPermission -> {
            MetroText(
                text = stringResource(R.string.settings_shizuku_nudge_need_permission),
                style = MetroTextStyle.DialogBody,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            )
            MetroBorderButton(
                text = stringResource(R.string.settings_shizuku_allow),
                onClick = {
                    ShizukuAvailability.requestPermission()
                    onStateChanged()
                },
                modifier = Modifier.padding(horizontal = 12.dp),
                fontSize = 15.sp,
            )
        }
        ShizukuReadyState.Ready -> {
            MetroText(
                text = stringResource(R.string.settings_shizuku_ready_hint),
                style = MetroTextStyle.DialogBody,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 4.dp),
            )
        }
    }
}
