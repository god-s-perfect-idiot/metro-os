package com.metro.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.system.MetroClockFace
import com.metro.system.MetroClockFaceParts
import com.metro.system.MetroTileWidgetFace
import com.metro.system.MetroTileWidgetFaceKind
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTileWidgetGlyphs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/**
 * Renders [MetroTileWidgetFace] on Start — shared contract for catalog widgets and future
 * pin-capable custom tiles. Clocks tick locally; battery / glyph state comes from the face payload.
 */
@Composable
fun StartWidgetFace(
    face: MetroTileWidgetFace,
    contentColor: Color,
    tileBackground: Color,
    modifier: Modifier = Modifier,
    /** 1×1 / 2×2 Start sizes — left-bottom AM/PM and tighter digit scale. */
    compact: Boolean = true,
) {
    when (face.kind) {
        MetroTileWidgetFaceKind.DIGITAL_CLOCK -> {
            var parts by remember { mutableStateOf(MetroClockFace.parts()) }
            LaunchedEffect(Unit) {
                while (true) {
                    parts = MetroClockFace.parts()
                    val now = LocalDateTime.now()
                    val delayMs = ((60 - now.second) * 1000L - now.nano / 1_000_000L)
                        .coerceAtLeast(250L)
                    delay(delayMs)
                }
            }
            DigitalClockWidgetFace(
                parts = parts,
                contentColor = contentColor,
                compact = compact,
                modifier = modifier,
            )
        }
        MetroTileWidgetFaceKind.ANALOG_CLOCK -> {
            var parts by remember { mutableStateOf(MetroClockFace.parts()) }
            LaunchedEffect(Unit) {
                while (true) {
                    parts = MetroClockFace.parts()
                    val now = LocalDateTime.now()
                    val delayMs = ((60 - now.second) * 1000L - now.nano / 1_000_000L)
                        .coerceAtLeast(250L)
                    delay(delayMs)
                }
            }
            AnalogClockWidgetFace(parts = parts, contentColor = contentColor, modifier = modifier)
        }
        MetroTileWidgetFaceKind.BATTERY -> {
            BatteryWidgetFace(
                percent = face.batteryPercent ?: 0,
                contentColor = contentColor,
                modifier = modifier.padding(6.dp),
            )
        }
        MetroTileWidgetFaceKind.GLYPH,
        MetroTileWidgetFaceKind.GLYPH_TOGGLE,
        -> {
            val on = face.kind == MetroTileWidgetFaceKind.GLYPH_TOGGLE && face.toggleOn == true
            val accent = MetroTheme.colors.accent
            // Transparent when the host tile is a Start wallpaper window; white when toggled on.
            val background = if (on) Color.White else tileBackground
            val glyphColor = when {
                on -> accent
                else -> contentColor
            }.copy(alpha = if (face.dimmed == true) 0.45f else 1f)
            val resId = MetroTileWidgetGlyphs.resId(face.glyph)
            Box(
                modifier = if (background == Color.Transparent) {
                    modifier
                } else {
                    modifier.background(background)
                },
                contentAlignment = Alignment.Center,
            ) {
                if (resId != null) {
                    Image(
                        painter = painterResource(resId),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(glyphColor),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                if (face.glyph == com.metro.system.MetroTileWidgetGlyph.LOCK) {
                                    4.dp
                                } else {
                                    18.dp
                                },
                            ),
                    )
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun DigitalClockWidgetFace(
    parts: MetroClockFaceParts,
    contentColor: Color,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val inset = min(maxWidth.value, maxHeight.value).coerceAtLeast(1f) *
            if (compact) 0.08f else 0.06f
        val widthBudget = (maxWidth.value - inset * 2f).coerceAtLeast(1f)
        // Leave a bottom band for AM/PM so digits never collide with the period.
        val periodBand = if (compact) {
            (maxHeight.value * 0.22f).coerceAtLeast(12f)
        } else {
            (maxHeight.value * 0.18f).coerceAtLeast(10f)
        }
        val heightBudget = (maxHeight.value - inset * 2f - periodBand).coerceAtLeast(1f)
        // Light Noto "12:59" is wider than Thin — use a conservative width ratio.
        val widthRatio = if (compact) 2.15f else 1.95f
        val fromWidth = widthBudget / widthRatio
        val fromHeight = heightBudget * 0.88f
        val timeSizePx = min(fromWidth, fromHeight).coerceIn(10f, 120f)
        val timeSize = timeSizePx.sp
        val periodSize = (timeSizePx * if (compact) 0.26f else 0.22f)
            .coerceIn(8f, timeSizePx * 0.4f)
            .sp
        val colonDot = (timeSizePx * 0.075f).coerceAtLeast(2f).dp
        val colonPad = (timeSizePx * 0.08f).dp
        // One step thicker than catalog Thin so digits stay readable at small sizes.
        val weight = FontWeight.Light
        val timeStyle = TextStyle(
            color = contentColor,
            fontSize = timeSize,
            lineHeight = timeSize,
            fontFamily = MetroTheme.fontFamily,
            fontWeight = weight,
        )
        val periodStyle = TextStyle(
            color = contentColor,
            fontSize = periodSize,
            lineHeight = periodSize,
            fontFamily = MetroTheme.fontFamily,
            fontWeight = weight,
        )
        val density = LocalDensity.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inset.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxSize()
                    .padding(bottom = periodBand.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    BasicText(
                        text = parts.hour,
                        style = timeStyle,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Column(
                        modifier = Modifier
                            .padding(horizontal = colonPad)
                            .height(with(density) { timeSize.toDp() * 0.40f }),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(modifier = Modifier.size(colonDot).background(contentColor))
                        Box(modifier = Modifier.size(colonDot).background(contentColor))
                    }
                    BasicText(
                        text = parts.minute,
                        style = timeStyle,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            BasicText(
                text = parts.period,
                style = periodStyle,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}

@Composable
private fun AnalogClockWidgetFace(
    parts: MetroClockFaceParts,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.padding(8.dp)) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        for (i in 0 until 60) {
            val isHour = i % 5 == 0
            val angleRad = Math.toRadians(i * 6.0 - 90.0)
            val outer = radius
            val inner = if (isHour) radius * 0.78f else radius * 0.90f
            val stroke = if (isHour) radius * 0.085f else radius * 0.028f
            val cosA = cos(angleRad).toFloat()
            val sinA = sin(angleRad).toFloat()
            drawLine(
                color = contentColor,
                start = Offset(center.x + cosA * inner, center.y + sinA * inner),
                end = Offset(center.x + cosA * outer, center.y + sinA * outer),
                strokeWidth = stroke,
                cap = StrokeCap.Butt,
            )
        }
        rotate(degrees = parts.hourHandDegrees, pivot = center) {
            drawLine(
                color = contentColor,
                start = center,
                end = Offset(center.x, center.y - radius * 0.48f),
                strokeWidth = radius * 0.075f,
                cap = StrokeCap.Butt,
            )
        }
        rotate(degrees = parts.minuteHandDegrees, pivot = center) {
            drawLine(
                color = contentColor,
                start = center,
                end = Offset(center.x, center.y - radius * 0.72f),
                strokeWidth = radius * 0.055f,
                cap = StrokeCap.Butt,
            )
        }
    }
}

@Composable
private fun BatteryWidgetFace(
    percent: Int,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val side = minOf(maxWidth, maxHeight)
        val digits = percent.coerceIn(0, 100).toString()
        val glyphWidth = side * 0.26f
        val glyphHeight = side * 0.52f
        val gap = side * 0.12f
        val textBudget = (maxWidth - glyphWidth - gap).coerceAtLeast(12.dp)
        val perDigit = textBudget / digits.length.coerceAtLeast(1)
        val fontSize = (perDigit.value * 1.08f)
            .coerceIn(20f, (side.value * 0.56f))
            .sp
        val fraction = percent.coerceIn(0, 100) / 100f
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Canvas(modifier = Modifier.size(width = glyphWidth, height = glyphHeight)) {
                val stroke = size.width * 0.12f
                val capHeight = size.height * 0.08f
                val capWidth = size.width * 0.42f
                val bodyTop = capHeight + size.height * 0.04f
                val bodyHeight = size.height - bodyTop
                drawRect(
                    color = contentColor,
                    topLeft = Offset((size.width - capWidth) / 2f, 0f),
                    size = Size(capWidth, capHeight),
                )
                drawRect(
                    color = contentColor,
                    topLeft = Offset(0f, bodyTop),
                    size = Size(size.width, bodyHeight),
                    style = Stroke(width = stroke),
                )
                val inset = stroke * 1.4f
                val fillTrackHeight = bodyHeight - inset * 2f
                val fillHeight = fillTrackHeight * fraction
                if (fillHeight > 0f) {
                    drawRect(
                        color = contentColor,
                        topLeft = Offset(inset, size.height - inset - fillHeight),
                        size = Size(size.width - inset * 2f, fillHeight),
                    )
                }
            }
            Spacer(modifier = Modifier.width(gap))
            BasicText(
                text = digits,
                style = TextStyle(
                    color = contentColor,
                    fontSize = fontSize,
                    lineHeight = fontSize,
                    fontFamily = MetroTheme.fontFamily,
                    fontWeight = FontWeight.Normal,
                ),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
