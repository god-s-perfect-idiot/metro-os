package com.metro.statusbar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.metro.statusbar.TrayIndicator
import com.metro.statusbar.TraySnapshot
import com.metro.statusbar.TraySpec
import com.metro.statusbar.TrayVisibilityMode
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle

@Composable
fun StatusTray(
    snapshot: TraySnapshot,
    onTrayTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (snapshot.theme.visibilityMode == TrayVisibilityMode.Hidden) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TraySpec.TRAY_HEIGHT_DP.dp)
            .background(snapshot.theme.backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTrayTap,
            )
            .padding(horizontal = 12.dp)
            .testTag("metro_status_tray"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedVisibility(
                visible = snapshot.expanded,
                enter = slideInHorizontally(
                    animationSpec = tween(TraySpec.EXPAND_ANIMATION_MS.toInt()),
                    initialOffsetX = { -it / 2 },
                ) + fadeIn(tween(TraySpec.EXPAND_ANIMATION_MS.toInt())),
                exit = slideOutHorizontally(
                    animationSpec = tween(TraySpec.COLLAPSE_ANIMATION_MS.toInt()),
                    targetOffsetX = { -it / 2 },
                ) + fadeOut(tween(TraySpec.COLLAPSE_ANIMATION_MS.toInt())),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    snapshot.indicators.forEach { indicator ->
                        TrayIndicatorGlyph(
                            indicator = indicator,
                            color = snapshot.theme.foregroundColor,
                        )
                    }
                }
            }
            if (snapshot.showProgress) {
                TrayProgressSpinner(color = snapshot.theme.accentColor)
            }
        }
        MetroText(
            text = snapshot.clockText,
            style = MetroTextStyle.Body,
            color = snapshot.theme.foregroundColor,
            modifier = Modifier.semantics {
                contentDescription = "Clock"
            },
        )
    }
}

@Composable
private fun TrayIndicatorGlyph(
    indicator: TrayIndicator,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(14.dp)) {
        when (indicator) {
            TrayIndicator.Cellular -> {
                val barWidth = size.width * 0.12f
                val gap = size.width * 0.08f
                repeat(4) { index ->
                    val height = size.height * (0.35f + index * 0.15f)
                    drawRect(
                        color = color,
                        topLeft = Offset(index * (barWidth + gap), size.height - height),
                        size = androidx.compose.ui.geometry.Size(barWidth, height),
                    )
                }
            }
            TrayIndicator.Wifi -> {
                val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)
                repeat(3) { band ->
                    val radius = size.minDimension * (0.22f + band * 0.18f)
                    drawArc(
                        color = color,
                        startAngle = 225f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(size.width / 2f - radius, size.height / 2f - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                        style = stroke,
                    )
                }
            }
            TrayIndicator.Bluetooth -> {
                val stroke = Stroke(width = size.minDimension * 0.1f, cap = StrokeCap.Round)
                val cx = size.width / 2f
                drawLine(color, Offset(cx, size.height * 0.1f), Offset(cx, size.height * 0.9f), stroke.width)
                drawLine(color, Offset(cx, size.height * 0.35f), Offset(cx + size.width * 0.2f, size.height * 0.5f), stroke.width)
                drawLine(color, Offset(cx, size.height * 0.65f), Offset(cx + size.width * 0.2f, size.height * 0.5f), stroke.width)
            }
            TrayIndicator.Alarm -> {
                val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)
                drawCircle(color, size.minDimension * 0.28f, center = Offset(size.width / 2f, size.height / 2f), style = stroke)
                drawLine(color, Offset(size.width / 2f, size.height / 2f), Offset(size.width / 2f, size.height * 0.35f), stroke.width)
                drawLine(color, Offset(size.width / 2f, size.height / 2f), Offset(size.width * 0.62f, size.height * 0.58f), stroke.width)
            }
            TrayIndicator.Location -> {
                val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)
                drawCircle(color, size.minDimension * 0.22f, center = Offset(size.width / 2f, size.height * 0.42f), style = stroke)
                drawCircle(color, size.minDimension * 0.05f, center = Offset(size.width / 2f, size.height * 0.42f))
                drawLine(color, Offset(size.width / 2f, size.height * 0.62f), Offset(size.width / 2f, size.height * 0.9f), stroke.width)
            }
            TrayIndicator.Battery -> {
                val bodyWidth = size.width * 0.72f
                val bodyHeight = size.height * 0.48f
                val left = (size.width - bodyWidth) / 2f
                val top = (size.height - bodyHeight) / 2f
                val stroke = Stroke(width = size.minDimension * 0.08f)
                drawRect(color, topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(bodyWidth, bodyHeight), style = stroke)
                drawRect(
                    color = color,
                    topLeft = Offset(left + bodyWidth, top + bodyHeight * 0.25f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.08f, bodyHeight * 0.5f),
                )
                drawRect(
                    color = color,
                    topLeft = Offset(left + bodyWidth * 0.18f, top + bodyHeight * 0.22f),
                    size = androidx.compose.ui.geometry.Size(bodyWidth * 0.55f, bodyHeight * 0.56f),
                )
            }
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
        Canvas(modifier = Modifier.size(14.dp)) {
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
