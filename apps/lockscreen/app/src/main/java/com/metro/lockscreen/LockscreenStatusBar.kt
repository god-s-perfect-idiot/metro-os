package com.metro.lockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.metro.system.MetroStatusBar
import com.metro.ui.MetroColors
import com.metro.ui.MetroWifiBandCount
import com.metro.ui.drawMetroWifiGlyph
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Horizontal insets match the Metro system tray (physical L/R, not RTL). */
private val StartPadding = 10.dp
private val EndPadding = 2.dp
private val CellularGlyphHeight = 13.dp
private val CellularGlyphWidth = 18.dp
private val WifiGlyphHeight = 13.dp
private val WifiGlyphWidth = 13.dp
private val BatteryWidth = 29.dp
private val BatteryHeight = 14.dp
private val WifiLeadingPadding = 8.dp
/**
 * Invisible slot matching the Metro tray clock (`h:mm` at 14sp) so battery stays left of
 * where the clock would sit — lock chrome owns the large time; tray has no clock digits.
 */
private val ClockReserveWidth = 40.dp

/**
 * WP8.1 lock-screen status tray: always-visible indicators with **no background**, so they
 * sit directly on the lock fill / wallpaper. No clock digits (lock chrome owns time), but the
 * right cluster keeps a clock-width gap after battery so layout matches the Metro tray.
 *
 * Vertically: row height is the system status-bar / cutout band (same region as
 * `com.metro.statusbar`); icons are centered in that band.
 */
@Composable
fun LockscreenStatusBar(
    color: Color,
    topInsetPx: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var battery by remember {
        mutableStateOf(LockscreenBatterySource.current(context))
    }
    var signal by remember {
        mutableStateOf(LockscreenSignalSource.current(context))
    }
    var phoneStateGranted by remember {
        mutableStateOf(LockscreenSignalSource.canReadCellular(context))
    }

    // Pick up late READ_PHONE_STATE grants without tearing down the overlay.
    LaunchedEffect(context) {
        while (isActive) {
            delay(2_000L)
            val granted = LockscreenSignalSource.canReadCellular(context)
            if (granted != phoneStateGranted) {
                phoneStateGranted = granted
            }
            signal = LockscreenSignalSource.current(context)
        }
    }

    DisposableEffect(context, phoneStateGranted) {
        val appContext = context.applicationContext
        val refreshSignal = {
            signal = LockscreenSignalSource.current(appContext)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        battery = LockscreenBatterySource.parse(intent)
                    }
                    WifiManager.RSSI_CHANGED_ACTION,
                    WifiManager.NETWORK_STATE_CHANGED_ACTION,
                    WifiManager.WIFI_STATE_CHANGED_ACTION,
                    -> refreshSignal()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        battery = LockscreenBatterySource.current(appContext)
        refreshSignal()

        val telephonyHandle = LockscreenSignalSource.registerCellularListener(appContext) { bars ->
            signal = signal.copy(cellularBars = bars)
        }

        onDispose {
            runCatching { appContext.unregisterReceiver(receiver) }
            telephonyHandle?.unregister()
        }
    }

    val statusBarsPx = WindowInsets.statusBars.getTop(density)
    val cutoutPx = WindowInsets.displayCutout.getTop(density)
    val safeTopPx = maxOf(statusBarsPx, cutoutPx, topInsetPx)
    val bandHeightDp = with(density) {
        maxOf(safeTopPx.toDp(), MetroStatusBar.HEIGHT_DP.dp)
    }
    val inactive = color.copy(alpha = 0.35f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(bandHeightDp)
            .absolutePadding(left = StartPadding, right = EndPadding)
            .testTag("metro_lockscreen_status_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Canvas(modifier = Modifier.size(CellularGlyphWidth, CellularGlyphHeight)) {
                drawCellularBars(
                    filled = signal.cellularBars,
                    color = color,
                    inactiveColor = inactive,
                )
            }
            val wifiBands = signal.wifiBands
            if (wifiBands != null) {
                Canvas(
                    modifier = Modifier
                        .padding(start = WifiLeadingPadding)
                        .size(WifiGlyphWidth, WifiGlyphHeight),
                ) {
                    drawMetroWifiGlyph(
                        color = color,
                        inactiveColor = inactive,
                        filledBands = wifiBands.coerceIn(0, MetroWifiBandCount),
                        bandCount = MetroWifiBandCount,
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (battery.present) {
                Canvas(
                    modifier = Modifier
                        .size(BatteryWidth, BatteryHeight)
                        // Offscreen layer so BlendMode.Clear punches a true hole for the plug.
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                ) {
                    drawLockBattery(
                        battery = battery,
                        color = color,
                    )
                }
            }
            // Keep battery clear of the trailing edge the Metro tray clock would occupy.
            Spacer(modifier = Modifier.width(ClockReserveWidth))
        }
    }
}

private fun DrawScope.drawCellularBars(
    filled: Int,
    color: Color,
    inactiveColor: Color,
) {
    val w = size.width
    val h = size.height
    val barWidth = w * 0.20f
    val gap = w * 0.065f
    val count = LockscreenSignalStatus.CELLULAR_BAR_COUNT
    val filledBars = filled.coerceIn(0, count)
    repeat(count) { index ->
        val barHeight = h * (0.4f + index * 0.2f)
        drawRect(
            color = if (index < filledBars) color else inactiveColor,
            topLeft = Offset(index * (barWidth + gap), h - barHeight),
            size = Size(barWidth, barHeight),
        )
    }
}

private data class ChargingPlugLayout(
    val topGapLeft: Float,
    val topGapRight: Float,
    val bottomGapLeft: Float,
    val bottomGapRight: Float,
    val outlineWidth: Float,
    val path: Path,
)

private fun chargingPlugLayout(
    centerX: Float,
    bodyTop: Float,
    bodyBottom: Float,
    bodyWidth: Float,
    strokeWidth: Float,
): ChargingPlugLayout {
    val bodyHeight = bodyBottom - bodyTop
    val plugW = bodyWidth * 0.32f
    val prongW = strokeWidth * 1.15f
    val prongGap = plugW * 0.30f
    val cordW = strokeWidth * 1.25f
    val corner = CornerRadius(plugW * 0.18f, plugW * 0.18f)
    val outlineWidth = strokeWidth * 1.15f

    val headH = plugW * 0.95f
    val headTop = bodyTop - strokeWidth * 0.5f
    val headBottom = headTop + headH
    val headLeft = centerX - plugW / 2f
    val headRight = headLeft + plugW

    val prongH = (headTop - 0f).coerceIn(bodyHeight * 0.22f, bodyHeight * 0.38f)
    val prongTop = headTop - prongH
    val leftProngX = centerX - prongGap / 2f - prongW
    val rightProngX = centerX + prongGap / 2f

    val cordTop = headBottom - strokeWidth * 0.25f
    val cordBottom = bodyBottom + strokeWidth * 0.5f - outlineWidth * 0.5f
    val cordH = (cordBottom - cordTop).coerceAtLeast(strokeWidth)

    val gapPad = outlineWidth * 0.5f + strokeWidth * 0.08f
    val path = Path().apply {
        addRect(Rect(Offset(leftProngX, prongTop), Size(prongW, prongH)))
        addRect(Rect(Offset(rightProngX, prongTop), Size(prongW, prongH)))
        addRoundRect(
            RoundRect(
                left = headLeft,
                top = headTop,
                right = headRight,
                bottom = headBottom,
                cornerRadius = corner,
            ),
        )
        addRect(Rect(Offset(centerX - cordW / 2f, cordTop), Size(cordW, cordH)))
    }
    return ChargingPlugLayout(
        topGapLeft = headLeft - gapPad,
        topGapRight = headRight + gapPad,
        bottomGapLeft = centerX - cordW / 2f - gapPad,
        bottomGapRight = centerX + cordW / 2f + gapPad,
        outlineWidth = outlineWidth,
        path = path,
    )
}

private fun DrawScope.drawBatteryOutlineWithPlugGaps(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    topGapLeft: Float,
    topGapRight: Float,
    bottomGapLeft: Float,
    bottomGapRight: Float,
    strokeWidth: Float,
    color: Color,
) {
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt, join = StrokeJoin.Miter)
    val leftShell = Path().apply {
        moveTo(topGapLeft, top)
        lineTo(left, top)
        lineTo(left, bottom)
        lineTo(bottomGapLeft, bottom)
    }
    val rightShell = Path().apply {
        moveTo(topGapRight, top)
        lineTo(right, top)
        lineTo(right, bottom)
        lineTo(bottomGapRight, bottom)
    }
    drawPath(leftShell, color = color, style = stroke)
    drawPath(rightShell, color = color, style = stroke)
}

private fun DrawScope.drawLockBattery(
    battery: LockscreenBatteryStatus,
    color: Color,
) {
    val bodyWidth = size.width * 0.898f
    val bodyHeight = size.height * 0.70f
    val left = 0f
    val top = (size.height - bodyHeight) / 2f
    val right = left + bodyWidth
    val bottom = top + bodyHeight
    val strokeWidth = bodyHeight * 0.13f
    val fillColor = if (battery.isLow) MetroColors.AccentRed else color

    val plug = if (battery.charging) {
        chargingPlugLayout(
            centerX = left + bodyWidth * 0.5f,
            bodyTop = top,
            bodyBottom = bottom,
            bodyWidth = bodyWidth,
            strokeWidth = strokeWidth,
        )
    } else {
        null
    }

    val inset = strokeWidth * 1.55f
    val fillTrackWidth = bodyWidth - inset * 2f
    val fillWidth = fillTrackWidth * battery.fraction.coerceIn(0f, 1f)
    if (fillWidth > 0f) {
        drawRect(
            color = fillColor,
            topLeft = Offset(left + inset, top + inset),
            size = Size(fillWidth, bodyHeight - inset * 2f),
        )
    }

    // Punch through the charge fill so the plug sits on the lock wallpaper / accent — no
    // painted "carve" color (that read as a black border on the transparent lock tray).
    if (plug != null) {
        drawPath(plug.path, color = Color.Black, blendMode = BlendMode.Clear)
    }

    if (plug != null) {
        drawBatteryOutlineWithPlugGaps(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            topGapLeft = plug.topGapLeft,
            topGapRight = plug.topGapRight,
            bottomGapLeft = plug.bottomGapLeft,
            bottomGapRight = plug.bottomGapRight,
            strokeWidth = strokeWidth,
            color = color,
        )
    } else {
        drawRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(bodyWidth, bodyHeight),
            style = Stroke(width = strokeWidth),
        )
    }

    val nubWidth = size.width - bodyWidth
    drawRect(
        color = color,
        topLeft = Offset(right, top + bodyHeight * 0.30f),
        size = Size(nubWidth, bodyHeight * 0.40f),
    )

    if (plug != null) {
        drawPath(plug.path, color = color)
    }
}
