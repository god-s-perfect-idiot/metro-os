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
import androidx.compose.ui.geometry.Offset
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

private val GlyphHeight = 13.dp
private val GlyphWidth = 15.dp
private val DataGlyphWidth = 20.dp
private val BatteryWidth = 22.dp

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
            .padding(horizontal = 10.dp)
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                snapshot.indicators.forEach { indicator ->
                    TrayIndicatorItem(
                        indicator = indicator,
                        color = foreground,
                        backgroundColor = background,
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (snapshot.showProgress) {
                TrayProgressSpinner(color = snapshot.theme.accentColor)
            }
            if (snapshot.battery.present) {
                TrayBatteryGlyph(battery = snapshot.battery, color = foreground)
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
private fun TrayIndicatorItem(
    indicator: TrayIndicator,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val width = if (indicator == TrayIndicator.DataConnection) DataGlyphWidth else GlyphWidth
    Canvas(modifier = modifier.size(width = width, height = GlyphHeight)) {
        drawIndicator(indicator, color, backgroundColor)
    }
}

private fun DrawScope.drawIndicator(
    indicator: TrayIndicator,
    color: Color,
    backgroundColor: Color,
) {
    val w = size.width
    val h = size.height
    when (indicator) {
        TrayIndicator.Cellular -> {
            val barWidth = w * 0.16f
            val gap = w * 0.11f
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
            drawContext.canvas.nativeCanvas.drawText(
                "4G",
                w / 2f,
                h * 0.86f,
                Paint().apply {
                    this.color = color.toArgb()
                    textSize = h * 1.05f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
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
            val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
            repeat(3) { band ->
                val radius = w * (0.18f + band * 0.16f)
                drawArc(
                    color = color,
                    startAngle = 225f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w / 2f - radius, h * 0.82f - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = stroke,
                )
            }
            drawCircle(color, w * 0.05f, Offset(w / 2f, h * 0.82f))
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

@Composable
private fun TrayBatteryGlyph(
    battery: BatteryStatus,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = BatteryWidth, height = GlyphHeight)) {
        val bodyWidth = size.width * 0.82f
        val bodyHeight = size.height * 0.5f
        val left = 0f
        val top = (size.height - bodyHeight) / 2f
        val stroke = Stroke(width = size.height * 0.08f)
        drawRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(bodyWidth, bodyHeight),
            style = stroke,
        )
        drawRect(
            color = color,
            topLeft = Offset(left + bodyWidth, top + bodyHeight * 0.28f),
            size = Size(size.width * 0.08f, bodyHeight * 0.44f),
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
            val cx = left + bodyWidth * 0.6f
            val cy = size.height / 2f
            val bh = bodyHeight * 0.66f
            val bw = bodyWidth * 0.12f
            val bolt = Path().apply {
                moveTo(cx + bw * 0.4f, cy - bh)
                lineTo(cx - bw, cy + bh * 0.2f)
                lineTo(cx, cy + bh * 0.2f)
                lineTo(cx - bw * 0.4f, cy + bh)
                lineTo(cx + bw, cy - bh * 0.2f)
                lineTo(cx, cy - bh * 0.2f)
                close()
            }
            drawPath(bolt, color = color)
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
