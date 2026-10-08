package eu.kanade.presentation.more.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroListItem
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import eu.kanade.presentation.util.rememberRequestPackageInstallsPermissionState
import eu.kanade.tachiyomi.util.system.launchRequestPackageInstallsPermission
import eu.kanade.tachiyomi.util.system.telemetryIncluded
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

internal class PermissionStep : OnboardingStep {

    private var notificationGranted by mutableStateOf(false)
    private var batteryGranted by mutableStateOf(false)

    override val isComplete: Boolean = true

    @Composable
    override fun title(): String = "permissions"

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val privacyPreferences = remember { context.appGraph.privacyPreferences }
        val lifecycleOwner = LocalLifecycleOwner.current

        val installGranted = rememberRequestPackageInstallsPermissionState()

        DisposableEffect(lifecycleOwner.lifecycle) {
            val observer = object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                    batteryGranted = context.getSystemService<PowerManager>()!!
                        .isIgnoringBatteryOptimizations(context.packageName)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        Column {
            PermissionGrantRow(
                title = stringResource(MR.strings.onboarding_permission_install_apps).lowercase(),
                subtitle = stringResource(MR.strings.onboarding_permission_install_apps_description),
                granted = installGranted,
                onGrantClick = { context.launchRequestPackageInstallsPermission() },
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionRequester = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = {
                        // Result is re-checked on resume.
                    },
                )
                PermissionGrantRow(
                    title = stringResource(MR.strings.onboarding_permission_notifications).lowercase(),
                    subtitle = stringResource(MR.strings.onboarding_permission_notifications_description),
                    granted = notificationGranted,
                    onGrantClick = {
                        permissionRequester.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            }

            PermissionGrantRow(
                title = stringResource(MR.strings.onboarding_permission_ignore_battery_opts).lowercase(),
                subtitle = stringResource(
                    MR.strings.onboarding_permission_ignore_battery_opts_description,
                ),
                granted = batteryGranted,
                onGrantClick = {
                    @SuppressLint("BatteryLife")
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    context.startActivity(intent)
                },
            )

            if (!telemetryIncluded) return@Column

            Spacer(modifier = Modifier.height(12.dp))

            val crashlyticsPref = privacyPreferences.crashlytics
            val crashlytics by crashlyticsPref.collectAsState()
            MetroToggleSwitch(
                checked = crashlytics,
                onCheckedChange = crashlyticsPref::set,
                label = stringResource(MR.strings.onboarding_permission_crashlytics).lowercase(),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            MetroText(
                text = stringResource(MR.strings.onboarding_permission_crashlytics_description),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            val analyticsPref = privacyPreferences.analytics
            val analytics by analyticsPref.collectAsState()
            MetroToggleSwitch(
                checked = analytics,
                onCheckedChange = analyticsPref::set,
                label = stringResource(MR.strings.onboarding_permission_analytics).lowercase(),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            MetroText(
                text = stringResource(MR.strings.onboarding_permission_analytics_description),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }
    }

    @Composable
    private fun PermissionGrantRow(
        title: String,
        subtitle: String,
        granted: Boolean,
        onGrantClick: () -> Unit,
    ) {
        Column {
            MetroListItem(
                title = title,
                subtitle = subtitle,
                oneLineMinHeight = 64.dp,
                twoLineMinHeight = 72.dp,
                verticalPadding = 8.dp,
                titleColor = if (granted) MetroTheme.colors.accent else null,
            )
            if (!granted) {
                MetroBorderButton(
                    text = stringResource(MR.strings.onboarding_permission_action_grant).lowercase(),
                    onClick = onGrantClick,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
