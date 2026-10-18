package com.metro.statusbar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.metro.statusbar.BatteryStatus
import com.metro.statusbar.BluetoothAudioKind
import com.metro.statusbar.SignalBarsStatus
import com.metro.statusbar.TrayIconFlags
import com.metro.statusbar.TrayIndicatorOrder
import com.metro.statusbar.TrayLayout
import com.metro.statusbar.TrayLayoutSlot
import com.metro.statusbar.TraySnapshot
import com.metro.statusbar.TraySpec
import com.metro.statusbar.TrayThemeSnapshot
import com.metro.statusbar.TrayVisibilityMode
import com.metro.ui.MetroColors
import com.metro.ui.MetroTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Static sample values so every setup preview draws the full enabled glyph set without
 * depending on live radio / mute / hotspot / notification state.
 */
object TrayPreviewSamples {
    const val DATA_LABEL = "4G"
    /**
     * Non-installed sentinel so setup/configure previews show the notification slot without
     * loading this app's icon. [StatusTray] draws the Windows Start flag as the fallback mark.
     */
    const val NOTIFICATION_PACKAGE = "com.metro.preview.notification"
    val signalBars = SignalBarsStatus(
        cellularBars = SignalBarsStatus.CELLULAR_BAR_COUNT,
        wifiBands = SignalBarsStatus.WIFI_BAND_COUNT,
    )
    val battery = BatteryStatus(fraction = 0.72f, charging = false, present = true)
    val bluetoothAudio = BluetoothAudioKind.Headset
}

/**
 * Builds an always-expanded [TraySnapshot] that forces every enabled layout icon to draw
 * with sample telemetry. Shared by customise / icons / configure previews.
 */
fun trayPreviewSnapshot(
    clockText: String,
    flags: TrayIconFlags,
    layout: List<TrayLayoutSlot> = TrayLayout.DEFAULT,
    theme: TrayThemeSnapshot,
    notificationPackage: String? = null,
): TraySnapshot = TraySnapshot(
    clockText = clockText,
    expanded = true,
    showProgress = false,
    indicators = TrayIndicatorOrder.expanded,
    dataConnectionLabel = if (flags.network) TrayPreviewSamples.DATA_LABEL else null,
    signalBars = TrayPreviewSamples.signalBars.copy(
        wifiBands = if (flags.wifi) SignalBarsStatus.WIFI_BAND_COUNT else null,
    ),
    ringerMuted = flags.mute,
    hotspotActive = flags.hotspot,
    bluetoothAudio = if (flags.bluetoothAudio) TrayPreviewSamples.bluetoothAudio else null,
    notificationPackage = if (flags.notifications) notificationPackage else null,
    iconFlags = flags,
    layout = layout,
    battery = if (flags.battery) TrayPreviewSamples.battery else TrayPreviewSamples.battery.copy(present = false),
    theme = theme,
)

/**
 * Setup preview strip — same [StatusTray] renderer as the live overlay, always expanded,
 * with sample icon state (not live telemetry).
 */
@Composable
fun TrayPreview(
    flags: TrayIconFlags,
    clockText: String = remember {
        LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm"))
    },
    layout: List<TrayLayoutSlot> = TrayLayout.DEFAULT,
    modifier: Modifier = Modifier,
    leftPaddingDp: Int = TraySpec.START_PADDING_DP,
    rightPaddingDp: Int = TraySpec.END_PADDING_DP,
    /**
     * Live tray theme from [com.metro.statusbar.TrayState] so setup previews match fill /
     * glyph contrast (black on light accent / match-app fills). Falls back to dark chrome.
     */
    theme: TrayThemeSnapshot? = null,
    onSlotCentersChanged: ((List<Float>) -> Unit)? = null,
) {
    val resolvedTheme = theme ?: TrayThemeSnapshot(
        backgroundColor = MetroColors.DarkBackground,
        foregroundColor = MetroTheme.colors.primaryText,
        accentColor = MetroTheme.colors.accent,
        darkTheme = true,
        visibilityMode = TrayVisibilityMode.Opaque,
        backdropColor = MetroColors.DarkBackground,
    )
    val snapshot = remember(flags, layout, clockText, resolvedTheme) {
        trayPreviewSnapshot(
            clockText = clockText,
            flags = flags,
            layout = layout,
            theme = resolvedTheme,
            notificationPackage = TrayPreviewSamples.NOTIFICATION_PACKAGE,
        )
    }
    StatusTray(
        snapshot = snapshot,
        onTrayTap = {},
        leftPaddingDp = leftPaddingDp,
        rightPaddingDp = rightPaddingDp,
        onSlotCentersChanged = onSlotCentersChanged,
        modifier = modifier,
    )
}
