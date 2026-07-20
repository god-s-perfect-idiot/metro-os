package com.metro.statusbar.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.metro.statusbar.BatteryStatus
import com.metro.statusbar.TrayIndicator
import com.metro.statusbar.TraySnapshot
import com.metro.statusbar.TraySpec
import com.metro.statusbar.TrayVisibilityMode
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle

private val GlyphHeight = 14.dp
private val GlyphWidth = 16.dp
private val WifiGlyphHeight = 17.dp
private val WifiGlyphWidth = 23.dp
private val DataGlyphWidth = 22.dp
// WP8.1 battery sits close to clock cap height, with a slightly longer and shallower silhouette.
private val BatteryWidth = 29.dp
private val BatteryHeight = 14.dp

/**
 * WP8.1 system tray.
 *
 * Left = status indicator row (collapsed shows a small base set; tap reveals the full set).
 * Right = battery + clock (always visible). [barHeightDp] lets the overlay fill the whole system
 * status-bar region (including notch/cutout) so nothing of the Android bar peeks through; defaults
 * to the WP 32dp strip for in-app previews.
 */
@Composable
fun StatusTray(
    snapshot: TraySnapshot,
    onTrayTap: () -> Unit,
    modifier: Modifier = Modifier,
    barHeightDp: Int = TraySpec.TRAY_HEIGHT_DP,
    startPaddingDp: Int = TraySpec.START_PADDING_DP,
    endPaddingDp: Int = TraySpec.END_PADDING_DP,
) {
    if (snapshot.theme.visibilityMode == TrayVisibilityMode.Hidden) return

    val foreground = snapshot.theme.foregroundColor
    val background = snapshot.theme.backgroundColor

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeightDp.dp)
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTrayTap,
            )
            .padding(start = startPaddingDp.dp, end = endPaddingDp.dp)
            .testTag("metro_status_tray"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Crossfade(
            targetState = snapshot.expanded,
            animationSpec = tween(TraySpec.EXPAND_ANIMATION_MS.toInt()),
            modifier = Modifier.weight(1f),
            label = "tray_indicators",
        ) { _ ->
            TrayIndicatorRow(
                indicators = snapshot.indicators,
                color = foreground,
                backgroundColor = background,
                dataConnectionLabel = snapshot.dataConnectionLabel,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (snapshot.showProgress) {
                TrayProgressSpinner(color = snapshot.theme.accentColor)
            }
            if (snapshot.battery.present) {
                TrayBatteryGlyph(
                    battery = snapshot.battery,
                    color = foreground,
                    backgroundColor = background,
                )
            }
            MetroText(
                text = snapshot.clockText,
                style = MetroTextStyle.Body,
                color = foreground,
                modifier = Modifier.semantics { contentDescription = "Clock" },
            )
        }
    }
}

@Composable
private fun TrayIndicatorRow(
    indicators: List<TrayIndicator>,
    color: Color,
    backgroundColor: Color,
    dataConnectionLabel: String?,
) {
    Row(
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TraySpec.CELLULAR_DATA_LABEL_GAP_DP.dp),
                ) {
                    TrayIndicatorItem(
                        indicator = TrayIndicator.Cellular,
                        color = color,
                        backgroundColor = backgroundColor,
                    )
                    TrayIndicatorItem(
                        indicator = TrayIndicator.DataConnection,
                        color = color,
                        backgroundColor = backgroundColor,
                        dataConnectionLabel = dataConnectionLabel,
                    )
                }
                index += 2
            } else {
                if (indicator == TrayIndicator.DataConnection && dataConnectionLabel == null) {
                    index++
                    continue
                }
                TrayIndicatorItem(
                    indicator = indicator,
                    color = color,
                    backgroundColor = backgroundColor,
                    dataConnectionLabel = dataConnectionLabel,
                )
                index++
            }
        }
    }
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
                    textSize = h * 1.1f
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
            // WP8.1 Wi-Fi keeps the quarter-arc silhouette, but the bands end flat rather than
            // using the softer Android rounded stroke treatment.
            val stroke = Stroke(width = w * 0.102f, cap = StrokeCap.Butt)
            val anchor = Offset(w * 0.84f, h * 0.86f)
            repeat(3) { band ->
                val radius = w * (0.15f + band * 0.165f)
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(anchor.x - radius, anchor.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = stroke,
                )
            }
            drawCircle(color, w * 0.077f, anchor)
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

/** WP8.1 charging overlay: two-prong plug with cord, outlined so it reads on the fill. */
private fun DrawScope.drawChargingPlug(
    centerX: Float,
    centerY: Float,
    bodyWidth: Float,
    bodyHeight: Float,
    fill: Color,
    outline: Color,
) {
    val plugW = bodyWidth * 0.34f
    val prongW = plugW * 0.17f
    val prongH = bodyHeight * 0.44f
    val gap = plugW * 0.20f
    val headH = bodyHeight * 0.26f
    val cordH = bodyHeight * 0.14f
    val corner = CornerRadius(prongW * 0.45f, prongW * 0.45f)
    val outlineW = maxOf(1.2f, size.height * 0.05f)

    val headTop = centerY - headH * 0.25f
    val prongTop = headTop - prongH + prongH * 0.12f
    val leftProngX = centerX - gap / 2f - prongW
    val rightProngX = centerX + gap / 2f
    val headLeft = centerX - plugW / 2f
    val cordW = prongW * 0.85f
    val cordTop = headTop + headH

    val plug = Path().apply {
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
    drawPath(plug, color = outline, style = Stroke(width = outlineW * 2f))
    drawPath(plug, color = fill)
}

@Composable
private fun TrayBatteryGlyph(
    battery: BatteryStatus,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = BatteryWidth, height = BatteryHeight)) {
        val bodyWidth = size.width * 0.898f
        val bodyHeight = size.height * 0.78f
        val left = 0f
        val top = (size.height - bodyHeight) / 2f
        val stroke = Stroke(width = bodyHeight * 0.078f)
        drawRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(bodyWidth, bodyHeight),
            style = stroke,
        )
        val nubWidth = size.width - bodyWidth
        drawRect(
            color = color,
            topLeft = Offset(left + bodyWidth, top + bodyHeight * 0.30f),
            size = Size(nubWidth, bodyHeight * 0.40f),
        )
        val inset = stroke.width * 1.8f
        val fillTrackWidth = bodyWidth - inset * 2f
        val fillWidth = fillTrackWidth * battery.fraction.coerceIn(0f, 1f)
        if (fillWidth > 0f) {
            drawRect(
                color = color,
                topLeft = Offset(left + inset, top + inset),
                size = Size(fillWidth, bodyHeight - inset * 2f),
            )
        }
        if (battery.charging) {
            drawChargingPlug(
                centerX = left + bodyWidth * 0.5f,
                centerY = top + bodyHeight / 2f,
                bodyWidth = bodyWidth,
                bodyHeight = bodyHeight,
                fill = color,
                outline = backgroundColor,
            )
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
