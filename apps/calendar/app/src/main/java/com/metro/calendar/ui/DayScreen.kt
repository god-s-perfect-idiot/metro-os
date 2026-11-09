package com.metro.calendar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.DayWeather
import com.metro.calendar.data.HourSlot
import com.metro.calendar.data.WeatherCondition
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/** Day schedule events — just under the previous 27sp day size. */
private val DayEventFontSize = 26.sp
private val DayEventLineHeight = 30.sp
private val DayHourFontSize = 20.sp
private val DayHourLineHeight = 24.sp
/** Time labels sit in this gutter; horizontal lines still run edge-to-edge under them. */
private val DayHourColumnWidth = 72.dp
private val EventTabWidth = 4.dp
private val EventTabHeight = 24.dp
private val HourRowMinHeight = 56.dp
private val DayGridStroke = 1.dp
/** WP8.1 day grid — light gray on black, visible but not harsh. */
private val DayGridLineColor = Color(0xFF6A6A6A)

@Composable
fun DayScreen(
    epochDay: Long,
    allDayEvents: List<CalendarEvent>,
    hourSlots: List<HourSlot>,
    weather: DayWeather?,
    useFahrenheit: Boolean,
    contextMenuEvent: CalendarEvent?,
    contextMenuFocusFraction: Float,
    onEventClick: (CalendarEvent) -> Unit,
    onEventLongPress: (CalendarEvent, Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MetroTheme.colors.accent

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(top = 4.dp, bottom = 8.dp),
    ) {
        // WP8.1 day chrome: weather sits top-right above the hour blocks.
        if (weather != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                MetroText(
                    text = weather.temperatureLabel(useFahrenheit),
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.primaryText,
                )
                WeatherGlyph(
                    condition = weather.condition,
                    tint = MetroTheme.colors.primaryText,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(28.dp),
                )
            }
        }

        // Day content scrolls together: all-day rows sit at the top of this day, then hours.
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (allDayEvents.isNotEmpty()) {
                items(allDayEvents, key = { "allday-${it.id}-${it.startMillis}" }) { event ->
                    DayEventRow(
                        title = event.title,
                        color = eventColor(event, accent),
                        contextMenuEvent = contextMenuEvent,
                        contextMenuFocusFraction = contextMenuFocusFraction,
                        event = event,
                        onEventClick = onEventClick,
                        onEventLongPress = onEventLongPress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
                item(key = "allday-spacer") {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(hourSlots, key = { "hour-${it.hour}" }) { slot ->
                HourRow(
                    slot = slot,
                    accent = accent,
                    drawBottomRule = slot.hour == hourSlots.lastOrNull()?.hour,
                    contextMenuEvent = contextMenuEvent,
                    contextMenuFocusFraction = contextMenuFocusFraction,
                    onEventClick = onEventClick,
                    onEventLongPress = onEventLongPress,
                )
            }
            item { Spacer(modifier = Modifier.height(96.dp)) }
        }
    }
}

@Composable
private fun HourRow(
    slot: HourSlot,
    accent: Color,
    drawBottomRule: Boolean,
    contextMenuEvent: CalendarEvent?,
    contextMenuFocusFraction: Float,
    onEventClick: (CalendarEvent) -> Unit,
    onEventLongPress: (CalendarEvent, Rect) -> Unit,
) {
    val eventCount = slot.events.size.coerceAtLeast(1)
    val contentMin = if (slot.events.isEmpty()) {
        HourRowMinHeight
    } else {
        HourRowMinHeight.coerceAtLeast((EventTabHeight + 16.dp) * eventCount)
    }

    val hourStyle = MetroTextStyle.ListItemSubtitle
        .toTextStyle(MetroTheme.fontFamily)
        .copy(
            color = MetroTheme.colors.secondaryText,
            fontSize = DayHourFontSize,
            lineHeight = DayHourLineHeight,
            textAlign = TextAlign.End,
        )
    val density = LocalDensity.current
    val strokePx = with(density) { DayGridStroke.toPx() }
    val gutterX = with(density) { DayHourColumnWidth.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = contentMin)
            .drawBehind {
                // Top hour rule — full bleed across the page (continues into neighbors).
                drawLine(
                    color = DayGridLineColor,
                    start = Offset(0f, strokePx / 2f),
                    end = Offset(size.width, strokePx / 2f),
                    strokeWidth = strokePx,
                )
                if (drawBottomRule) {
                    drawLine(
                        color = DayGridLineColor,
                        start = Offset(0f, size.height - strokePx / 2f),
                        end = Offset(size.width, size.height - strokePx / 2f),
                        strokeWidth = strokePx,
                    )
                }
                // Time / event gutter only — no outer box; horizontals run edge-to-edge.
                drawLine(
                    color = DayGridLineColor,
                    start = Offset(gutterX, 0f),
                    end = Offset(gutterX, size.height),
                    strokeWidth = strokePx,
                )
            },
        verticalAlignment = Alignment.Top,
    ) {
        BasicText(
            text = slot.label,
            style = hourStyle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .width(DayHourColumnWidth)
                .padding(start = 8.dp, end = 10.dp, top = 6.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
        ) {
            if (slot.events.isEmpty()) {
                Spacer(modifier = Modifier.height(1.dp))
            } else {
                slot.events.forEachIndexed { index, event ->
                    DayEventRow(
                        title = event.title,
                        color = eventColor(event, accent),
                        contextMenuEvent = contextMenuEvent,
                        contextMenuFocusFraction = contextMenuFocusFraction,
                        event = event,
                        onEventClick = onEventClick,
                        onEventLongPress = onEventLongPress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (index == 0) 0.dp else 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayEventRow(
    title: String,
    color: Color,
    event: CalendarEvent,
    contextMenuEvent: CalendarEvent?,
    contextMenuFocusFraction: Float,
    onEventClick: (CalendarEvent) -> Unit,
    onEventLongPress: (CalendarEvent, Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val boundsRef = remember(event.id, event.startMillis) { EventBoundsRef() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .calendarEventInteraction(
                isContextTarget = event.matchesContextTarget(contextMenuEvent),
                contextMenuFocusFraction = contextMenuFocusFraction,
                boundsRef = boundsRef,
                onClick = { onEventClick(event) },
                onLongPress = { bounds -> onEventLongPress(event, bounds) },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // WP8.1 accent tab on the left of the event.
        Box(
            modifier = Modifier
                .width(EventTabWidth)
                .height(EventTabHeight)
                .background(color),
        )
        CalendarLineText(
            text = title,
            style = MetroTextStyle.ListItemTitle,
            color = color,
            fontSize = DayEventFontSize,
            lineHeight = DayEventLineHeight,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        )
    }
}

@Composable
private fun WeatherGlyph(
    condition: WeatherCondition,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (condition) {
            WeatherCondition.Sunny -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.22f,
                    center = Offset(w * 0.5f, h * 0.42f),
                    style = Stroke(width = w * 0.08f),
                )
                val ray = w * 0.38f
                for (i in 0 until 8) {
                    val angle = Math.toRadians(i * 45.0)
                    val cx = w * 0.5f
                    val cy = h * 0.42f
                    val inner = w * 0.28f
                    drawLine(
                        color = tint,
                        start = Offset(
                            cx + (kotlin.math.cos(angle) * inner).toFloat(),
                            cy + (kotlin.math.sin(angle) * inner).toFloat(),
                        ),
                        end = Offset(
                            cx + (kotlin.math.cos(angle) * ray).toFloat(),
                            cy + (kotlin.math.sin(angle) * ray).toFloat(),
                        ),
                        strokeWidth = w * 0.07f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            WeatherCondition.Cloudy -> {
                drawCloud(tint, w, h * 0.55f, y = h * 0.42f)
            }
            WeatherCondition.Rain, WeatherCondition.Storm -> {
                drawCloud(tint, w, h * 0.5f, y = h * 0.32f)
                val drops = if (condition == WeatherCondition.Storm) 4 else 3
                for (i in 0 until drops) {
                    val x = w * (0.28f + i * 0.16f)
                    drawLine(
                        color = tint,
                        start = Offset(x, h * 0.58f),
                        end = Offset(x - w * 0.04f, h * 0.78f),
                        strokeWidth = w * 0.06f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloud(
    tint: Color,
    w: Float,
    cloudH: Float,
    y: Float,
) {
    val path = Path().apply {
        addOval(Rect(w * 0.18f, y - cloudH * 0.35f, w * 0.55f, y + cloudH * 0.35f))
        addOval(Rect(w * 0.38f, y - cloudH * 0.45f, w * 0.78f, y + cloudH * 0.25f))
        addOval(Rect(w * 0.55f, y - cloudH * 0.25f, w * 0.88f, y + cloudH * 0.4f))
    }
    drawPath(path, color = tint)
}

@Composable
private fun eventColor(event: CalendarEvent, accent: Color): Color {
    val hex = event.calendarColorHex.trim()
    if (hex.isEmpty()) return accent
    return remember(hex, accent) {
        runCatching {
            val cleaned = hex.removePrefix("#")
            Color(cleaned.toLong(16) or 0xFF000000L)
        }.getOrDefault(accent)
    }
}
