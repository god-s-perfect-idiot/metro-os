package com.metro.statusbar.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.statusbar.BatteryStatus
import com.metro.statusbar.TrayIndicator
import com.metro.statusbar.TrayIndicatorOrder
import com.metro.statusbar.TraySnapshot
import com.metro.statusbar.TraySpec
import com.metro.statusbar.TrayVisibilityMode
import com.metro.ui.MetroColors
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTransitions
import kotlin.math.abs

private val GlyphHeight = 14.dp
private val GlyphWidth = 16.dp
// Cellular signal bars: slightly wider and shorter than the shared glyph box.
private val CellularGlyphHeight = 13.dp
private val CellularGlyphWidth = 18.dp
private val WifiGlyphHeight = GlyphHeight
private val WifiGlyphWidth = 18.dp
private val DataGlyphWidth = 22.dp
// WP8.1 battery sits close to clock cap height, with a slightly longer and shallower silhouette.
private val BatteryWidth = 29.dp
private val BatteryHeight = 14.dp
/** Clock / data-label size for the 32dp tray — a step under dialog body (16sp). */
private val TrayClockFontSize = 14.sp
private val TrayClockLineHeight = 16.sp
/** Data connection label (4G, LTE, …) relative to the glyph box height. */
private const val DataLabelTextSizeFactor = 0.98f

/**
 * WP8.1 system tray.
 *
 * Default = clock only (right-aligned). Tap or going home drops the other icons in one-by-one
 * from above (right → left), holds briefly, then exits upward the same way. Swipe down opens the
 * Android notification shade and hides this overlay while the shade is expanded. [barHeightDp]
 * lets the overlay fill the whole system status-bar region (including notch/cutout); defaults to
 * the WP 32dp strip for in-app previews.
 */
@Composable
fun StatusTray(
    snapshot: TraySnapshot,
    onTrayTap: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeOpenNotifications: (() -> Unit)? = null,
    barHeightDp: Int = TraySpec.TRAY_HEIGHT_DP,
    /** Physical left inset (cutout / rounded corner); not RTL start. */
    leftPaddingDp: Int = TraySpec.START_PADDING_DP,
    /** Physical right inset (cutout / rounded corner / privacy dots); not RTL end. */
    rightPaddingDp: Int = TraySpec.END_PADDING_DP,
) {
    if (snapshot.theme.visibilityMode == TrayVisibilityMode.Hidden) return
    if (snapshot.notificationShadeOpen) return

    val foreground = snapshot.theme.foregroundColor
    val background = snapshot.theme.backgroundColor
    val density = LocalDensity.current
    val shadeOpenDragPx = with(density) { TraySpec.SHADE_OPEN_DRAG_DP.dp.toPx() }
    val leftVisible = remember(snapshot.dataConnectionLabel) {
        TrayIndicatorOrder.visibleLeft(snapshot.dataConnectionLabel)
    }
    val batteryPresent = snapshot.battery.present
    // Right → left: battery (if any) is reverseIndex 0; leftmost left-icon is highest delay.
    val batteryReverseIndex = 0
    val leftBatteryOffset = if (batteryPresent) 1 else 0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeightDp.dp)
            .clipToBounds()
            .background(background)
            .pointerInput(shadeOpenDragPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalDragY = 0f
                    var dragged = false
                    val pointerId = down.id
                    drag(pointerId) { change ->
                        val dy = change.positionChange().y
                        totalDragY += dy
                        if (abs(totalDragY) > viewConfiguration.touchSlop) {
                            dragged = true
                        }
                        if (dragged && onSwipeOpenNotifications != null && totalDragY > 0f) {
                            change.consume()
                        }
                    }
                    when {
                        dragged &&
                            onSwipeOpenNotifications != null &&
                            totalDragY >= shadeOpenDragPx -> onSwipeOpenNotifications.invoke()
                        !dragged -> onTrayTap()
                    }
                }
            }
            .absolutePadding(left = leftPaddingDp.dp, right = rightPaddingDp.dp)
            .testTag("metro_status_tray"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TrayIndicatorRow(
            indicators = leftVisible,
            expanded = snapshot.expanded,
            color = foreground,
            backgroundColor = background,
            dataConnectionLabel = snapshot.dataConnectionLabel,
            reverseIndexOffset = leftBatteryOffset,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clipToBounds(),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (snapshot.showProgress) {
                TrayProgressSpinner(color = snapshot.theme.accentColor)
            }
            AnimatedVisibility(
                visible = snapshot.expanded && batteryPresent,
                enter = trayIconEnter(batteryReverseIndex),
                exit = trayIconExit(batteryReverseIndex),
            ) {
                TrayBatteryGlyph(
                    battery = snapshot.battery,
                    color = foreground,
                )
            }
            BasicText(
                text = snapshot.clockText,
                style = MetroTextStyle.DialogBody.toTextStyle().copy(
                    color = foreground,
                    fontSize = TrayClockFontSize,
                    lineHeight = TrayClockLineHeight,
                ),
                modifier = Modifier.semantics { contentDescription = "Clock" },
            )
        }
    }
}

/** Drop from above; [reverseIndex] 0 = rightmost icon (starts first). */
private fun trayIconEnter(reverseIndex: Int) = slideInVertically(
    animationSpec = tween(
        durationMillis = MetroTransitions.StatusTrayExpandMs,
        delayMillis = reverseIndex * MetroTransitions.StatusTrayIconStaggerMs,
        easing = MetroTransitions.PageEasing,
    ),
    initialOffsetY = { fullHeight -> -fullHeight },
) + fadeIn(
    animationSpec = tween(
        durationMillis = MetroTransitions.StatusTrayExpandMs,
        delayMillis = reverseIndex * MetroTransitions.StatusTrayIconStaggerMs,
        easing = MetroTransitions.PageEasing,
    ),
)

/** Exit upward; same right → left stagger as enter. */
private fun trayIconExit(reverseIndex: Int) = slideOutVertically(
    animationSpec = tween(
        durationMillis = MetroTransitions.StatusTrayCollapseMs,
        delayMillis = reverseIndex * MetroTransitions.StatusTrayIconStaggerMs,
        easing = MetroTransitions.PageEasing,
    ),
    targetOffsetY = { fullHeight -> -fullHeight },
) + fadeOut(
    animationSpec = tween(
        durationMillis = MetroTransitions.StatusTrayCollapseMs,
        delayMillis = reverseIndex * MetroTransitions.StatusTrayIconStaggerMs,
        easing = MetroTransitions.PageEasing,
    ),
)

@Composable
private fun TrayIndicatorRow(
    indicators: List<TrayIndicator>,
    expanded: Boolean,
    color: Color,
    backgroundColor: Color,
    dataConnectionLabel: String?,
    reverseIndexOffset: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        var index = 0
        while (index < indicators.size) {
            val indicator = indicators[index]
            val next = indicators.getOrNull(index + 1)
            val showDataLabel = indicator == TrayIndicator.Cellular &&
                next == TrayIndicator.DataConnection &&
                dataConnectionLabel != null
            if (showDataLabel) {
                // Tight cellular+data cluster; each glyph still staggers on its own reverse index.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TraySpec.CELLULAR_DATA_LABEL_GAP_DP.dp),
                ) {
                    StaggeredTrayIcon(
                        visible = expanded,
                        reverseIndex = (indicators.size - 1 - index) + reverseIndexOffset,
                    ) {
                        TrayIndicatorItem(
                            indicator = TrayIndicator.Cellular,
                            color = color,
                            backgroundColor = backgroundColor,
                        )
                    }
                    StaggeredTrayIcon(
                        visible = expanded,
                        reverseIndex = (indicators.size - 1 - (index + 1)) + reverseIndexOffset,
                    ) {
                        TrayIndicatorItem(
                            indicator = TrayIndicator.DataConnection,
                            color = color,
                            backgroundColor = backgroundColor,
                            dataConnectionLabel = dataConnectionLabel,
                        )
                    }
                }
                index += 2
            } else {
                StaggeredTrayIcon(
                    visible = expanded,
                    reverseIndex = (indicators.size - 1 - index) + reverseIndexOffset,
                ) {
                    TrayIndicatorItem(
                        indicator = indicator,
                        color = color,
                        backgroundColor = backgroundColor,
                        dataConnectionLabel = dataConnectionLabel,
                        modifier = if (indicator == TrayIndicator.Wifi) {
                            Modifier.padding(start = TraySpec.WIFI_LEADING_PADDING_DP.dp)
                        } else {
                            Modifier
                        },
                    )
                }
                index++
            }
        }
    }
}

@Composable
private fun StaggeredTrayIcon(
    visible: Boolean,
    reverseIndex: Int,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = trayIconEnter(reverseIndex),
        exit = trayIconExit(reverseIndex),
        content = { content() },
    )
}

@Composable
private fun TrayIndicatorItem(
    indicator: TrayIndicator,
    color: Color,
    backgroundColor: Color,
    dataConnectionLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    val (width, height) = when (indicator) {
        TrayIndicator.Cellular -> CellularGlyphWidth to CellularGlyphHeight
        TrayIndicator.DataConnection -> DataGlyphWidth to GlyphHeight
        TrayIndicator.Wifi -> WifiGlyphWidth to WifiGlyphHeight
        else -> GlyphWidth to GlyphHeight
    }
    Canvas(modifier = modifier.size(width = width, height = height)) {
        drawIndicator(indicator, color, backgroundColor, dataConnectionLabel)
    }
}

private fun DrawScope.drawIndicator(
    indicator: TrayIndicator,
    color: Color,
    backgroundColor: Color,
    dataConnectionLabel: String? = null,
) {
    val w = size.width
    val h = size.height
    when (indicator) {
        TrayIndicator.Cellular -> {
            val barWidth = w * 0.20f
            val gap = w * 0.065f
            repeat(4) { index ->
                val barHeight = h * (0.4f + index * 0.2f)
                drawRect(
                    color = color,
                    topLeft = Offset(index * (barWidth + gap), h - barHeight),
                    size = Size(barWidth, barHeight),
                )
            }
        }
        TrayIndicator.DataConnection -> {
            val label = dataConnectionLabel ?: return
            drawContext.canvas.nativeCanvas.drawText(
                label,
                w / 2f,
                h * 0.86f,
                Paint().apply {
                    this.color = color.toArgb()
                    textSize = h * DataLabelTextSizeFactor
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                },
            )
        }
        TrayIndicator.CallForwarding -> {
            val stroke = h * 0.12f
            drawLine(color, Offset(w * 0.12f, h * 0.72f), Offset(w * 0.7f, h * 0.72f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.7f, h * 0.72f), Offset(w * 0.7f, h * 0.28f), stroke, StrokeCap.Round)
            val head = Path().apply {
                moveTo(w * 0.52f, h * 0.42f)
                lineTo(w * 0.7f, h * 0.22f)
                lineTo(w * 0.88f, h * 0.42f)
            }
            drawPath(head, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
        TrayIndicator.Roaming -> {
            val tri = Path().apply {
                moveTo(w * 0.5f, h * 0.18f)
                lineTo(w * 0.86f, h * 0.82f)
                lineTo(w * 0.14f, h * 0.82f)
                close()
            }
            drawPath(tri, color)
        }
        TrayIndicator.Wifi -> {
            // WP8.1 tray Wi-Fi: thick quarter-bands, flat ends, bottom-right origin.
            // Pack: dot | gap | band | gap | band | gap | band (stroke ≈ gap).
            // Origin diameter matches band stroke — WP references use a small hub, not a fat disc.
            val anchor = Offset(w * 0.94f, h * 0.94f)
            val avail = minOf(anchor.x, anchor.y)
            // Slightly denser than a strict 1:1 pack so bands read as bold as the Microsoft glyph.
            val strokeWidth = avail / 6.0f
            val gap = strokeWidth * 0.85f
            val dotR = strokeWidth * 0.5f
            val band0 = dotR + gap + strokeWidth * 0.5f
            val band1 = band0 + strokeWidth + gap
            val band2 = band1 + strokeWidth + gap
            // Keep outer half-stroke inside the canvas.
            val outerEdge = band2 + strokeWidth * 0.5f
            val scale = if (outerEdge > avail) avail / outerEdge else 1f
            for (radius in floatArrayOf(band0 * scale, band1 * scale, band2 * scale)) {
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(anchor.x - radius, anchor.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth * scale, cap = StrokeCap.Butt),
                )
            }
            drawCircle(color, dotR * scale, anchor)
        }
        TrayIndicator.Bluetooth -> {
            val stroke = h * 0.1f
            val cx = w / 2f
            drawLine(color, Offset(cx, h * 0.12f), Offset(cx, h * 0.88f), stroke, StrokeCap.Round)
            drawLine(color, Offset(cx, h * 0.12f), Offset(cx + w * 0.18f, h * 0.32f), stroke, StrokeCap.Round)
            drawLine(color, Offset(cx + w * 0.18f, h * 0.32f), Offset(cx - w * 0.04f, h * 0.5f), stroke, StrokeCap.Round)
            drawLine(color, Offset(cx, h * 0.88f), Offset(cx + w * 0.18f, h * 0.68f), stroke, StrokeCap.Round)
            drawLine(color, Offset(cx + w * 0.18f, h * 0.68f), Offset(cx - w * 0.04f, h * 0.5f), stroke, StrokeCap.Round)
        }
        TrayIndicator.QuietHours -> {
            // Crescent moon: a disc with an offset disc carved out in the tray background color.
            val r = h * 0.46f
            val center = Offset(w * 0.5f, h * 0.5f)
            drawCircle(color, r, center)
            drawCircle(backgroundColor, r * 0.92f, Offset(center.x + r * 0.6f, center.y - r * 0.18f))
        }
        TrayIndicator.DrivingMode -> {
            val stroke = h * 0.08f
            // Cabin + body of a small car.
            drawRoundRectPath(
                left = w * 0.22f, top = h * 0.28f, right = w * 0.78f, bottom = h * 0.55f, color = color,
            )
            drawRoundRectPath(
                left = w * 0.08f, top = h * 0.48f, right = w * 0.92f, bottom = h * 0.72f, color = color,
            )
            drawCircle(backgroundColor, w * 0.1f, Offset(w * 0.3f, h * 0.72f))
            drawCircle(backgroundColor, w * 0.1f, Offset(w * 0.7f, h * 0.72f))
            drawCircle(color, w * 0.06f, Offset(w * 0.3f, h * 0.72f))
            drawCircle(color, w * 0.06f, Offset(w * 0.7f, h * 0.72f))
            drawLine(color, Offset(w * 0.08f, h * 0.72f), Offset(w * 0.92f, h * 0.72f), stroke)
        }
        TrayIndicator.Ringer -> {
            // Vibrate: a small handset flanked by two motion arcs.
            drawRoundRectPath(left = w * 0.42f, top = h * 0.24f, right = w * 0.58f, bottom = h * 0.76f, color = color)
            val stroke = Stroke(width = w * 0.07f, cap = StrokeCap.Round)
            repeat(2) { side ->
                val dir = if (side == 0) -1f else 1f
                val radius = w * (0.16f + 0f)
                val cx = w / 2f + dir * w * 0.34f
                drawArc(
                    color = color,
                    startAngle = if (dir < 0) 300f else 120f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(cx - radius, h * 0.5f - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = stroke,
                )
            }
        }
        TrayIndicator.Location -> {
            val stroke = Stroke(width = h * 0.09f)
            drawCircle(color, w * 0.3f, Offset(w / 2f, h / 2f), style = stroke)
            drawCircle(color, w * 0.1f, Offset(w / 2f, h / 2f))
            drawLine(color, Offset(w / 2f, h * 0.04f), Offset(w / 2f, h * 0.22f), stroke.width)
            drawLine(color, Offset(w / 2f, h * 0.78f), Offset(w / 2f, h * 0.96f), stroke.width)
            drawLine(color, Offset(w * 0.04f, h / 2f), Offset(w * 0.22f, h / 2f), stroke.width)
            drawLine(color, Offset(w * 0.78f, h / 2f), Offset(w * 0.96f, h / 2f), stroke.width)
        }
        TrayIndicator.Battery -> Unit // Rendered separately on the right by TrayBatteryGlyph.
    }
}

/** Small filled-rect helper to keep the car/handset glyphs readable at tray sizes. */
private fun DrawScope.drawRoundRectPath(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    color: Color,
) {
    drawRect(color = color, topLeft = Offset(left, top), size = Size(right - left, bottom - top))
}

/**
 * WP8.1 charging overlay: solid two-prong plug with cord. Geometry is sized so prongs and cord
 * interrupt the battery outline (caller draws the casing with matching gaps).
 */
private data class ChargingPlugLayout(
    val gapLeft: Float,
    val gapRight: Float,
    val path: Path,
)

private fun chargingPlugLayout(
    centerX: Float,
    centerY: Float,
    bodyHeight: Float,
): ChargingPlugLayout {
    val plugW = bodyHeight * 0.78f
    val prongW = plugW * 0.20f
    val prongGap = plugW * 0.24f
    val prongH = bodyHeight * 0.48f
    val headH = bodyHeight * 0.40f
    val cordH = bodyHeight * 0.28f
    val cordW = prongW * 1.1f
    val corner = CornerRadius(prongW * 0.65f, prongW * 0.65f)

    // Head sits near vertical center; prongs/cord overhang so they cut the casing strokes.
    val headTop = centerY - headH * 0.42f
    val prongTop = headTop - prongH * 0.78f
    val leftProngX = centerX - prongGap / 2f - prongW
    val rightProngX = centerX + prongGap / 2f
    val headLeft = centerX - plugW / 2f
    val cordTop = headTop + headH - bodyHeight * 0.02f
    val gapPad = bodyHeight * 0.08f

    val path = Path().apply {
        addRect(Rect(Offset(leftProngX, prongTop), Size(prongW, prongH)))
        addRect(Rect(Offset(rightProngX, prongTop), Size(prongW, prongH)))
        addRoundRect(
            RoundRect(
                left = headLeft,
                top = headTop,
                right = headLeft + plugW,
                bottom = headTop + headH,
                cornerRadius = corner,
            ),
        )
        addRect(Rect(Offset(centerX - cordW / 2f, cordTop), Size(cordW, cordH)))
    }
    return ChargingPlugLayout(
        gapLeft = headLeft - gapPad,
        gapRight = headLeft + plugW + gapPad,
        path = path,
    )
}

/** Battery casing drawn as two open U-paths so the plug can interrupt top/bottom strokes. */
private fun DrawScope.drawBatteryOutlineWithPlugGap(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    gapLeft: Float,
    gapRight: Float,
    strokeWidth: Float,
    color: Color,
) {
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
    val leftShell = Path().apply {
        moveTo(gapLeft, top)
        lineTo(left, top)
        lineTo(left, bottom)
        lineTo(gapLeft, bottom)
    }
    val rightShell = Path().apply {
        moveTo(gapRight, top)
        lineTo(right, top)
        lineTo(right, bottom)
        lineTo(gapRight, bottom)
    }
    drawPath(leftShell, color = color, style = stroke)
    drawPath(rightShell, color = color, style = stroke)
}

@Composable
private fun TrayBatteryGlyph(
    battery: BatteryStatus,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = BatteryWidth, height = BatteryHeight)) {
        val bodyWidth = size.width * 0.898f
        val bodyHeight = size.height * 0.78f
        val left = 0f
        val top = (size.height - bodyHeight) / 2f
        val right = left + bodyWidth
        val bottom = top + bodyHeight
        val strokeWidth = bodyHeight * 0.078f
        val fillColor = if (battery.isLow) MetroColors.AccentRed else color

        val inset = strokeWidth * 1.8f
        val fillTrackWidth = bodyWidth - inset * 2f
        val fillWidth = fillTrackWidth * battery.fraction.coerceIn(0f, 1f)
        if (fillWidth > 0f) {
            drawRect(
                color = fillColor,
                topLeft = Offset(left + inset, top + inset),
                size = Size(fillWidth, bodyHeight - inset * 2f),
            )
        }

        val plug = if (battery.charging) {
            chargingPlugLayout(
                centerX = left + bodyWidth * 0.5f,
                centerY = top + bodyHeight / 2f,
                bodyHeight = bodyHeight,
            )
        } else {
            null
        }

        if (plug != null) {
            drawBatteryOutlineWithPlugGap(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                gapLeft = plug.gapLeft,
                gapRight = plug.gapRight,
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
}

@Composable
private fun TrayProgressSpinner(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(16.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(13.dp)) {
            rotate(degrees = -90f) {
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = size.minDimension * 0.12f, cap = StrokeCap.Round),
                )
            }
        }
    }
}
