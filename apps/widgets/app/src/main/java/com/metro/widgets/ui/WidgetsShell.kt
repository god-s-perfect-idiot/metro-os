package com.metro.widgets.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroColors
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import com.metro.widgets.R
import com.metro.widgets.data.BatterySnapshot
import com.metro.widgets.data.ClockFaceParts
import com.metro.widgets.data.NotifierTraySnapshot
import com.metro.widgets.data.StorageSnapshot
import com.metro.widgets.data.WidgetCatalog
import com.metro.widgets.data.WidgetFormatters
import com.metro.widgets.data.WidgetKind

private val WidgetGridGap = 8.dp
private val WidgetGridPadding = 12.dp
private val TileTitlePaddingH = 6.dp
private val TileTitlePaddingV = 4.dp

@Composable
fun WidgetsShell(
    state: WidgetsState,
    modifier: Modifier = Modifier,
) {
    DisposableEffect(state) {
        state.start()
        onDispose { state.stop() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .metroNavBarPadding()
            .background(Color.Black)
            .verticalScroll(rememberScrollState()),
    ) {
        MetroAppTitle(title = stringResource(R.string.widgets_app_title))
        Spacer(modifier = Modifier.height(12.dp))
        WidgetTileGrid(
            clock = state.clock,
            battery = state.battery,
            storage = state.storage,
            notifierTray = state.notifierTray,
            notifierAccessGranted = state.notifierAccessGranted,
            onRequestNotifierAccess = state::openNotifierAccessSettings,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun WidgetTileGrid(
    clock: ClockFaceParts,
    battery: BatterySnapshot,
    storage: StorageSnapshot,
    notifierTray: NotifierTraySnapshot,
    notifierAccessGranted: Boolean,
    onRequestNotifierAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.padding(horizontal = WidgetGridPadding)) {
        val columns = WidgetCatalog.COLUMNS
        val unit = (maxWidth - WidgetGridGap * (columns - 1)) / columns
        val maxBottom = WidgetCatalog.tiles.maxOf { it.gridRow + it.size.rowSpan }
        val contentHeight = unit * maxBottom + WidgetGridGap * (maxBottom - 1).coerceAtLeast(0)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight),
        ) {
            WidgetCatalog.tiles.forEach { kind ->
                val (w, h) = tilePixelSize(unit, kind.size.colSpan, kind.size.rowSpan)
                val x = (unit + WidgetGridGap) * kind.gridCol
                val y = (unit + WidgetGridGap) * kind.gridRow
                Box(
                    modifier = Modifier
                        .offset(x = x, y = y)
                        .size(width = w, height = h),
                ) {
                    WidgetTileFace(
                        kind = kind,
                        clock = clock,
                        battery = battery,
                        storage = storage,
                        notifierTray = notifierTray,
                        notifierAccessGranted = notifierAccessGranted,
                        onRequestNotifierAccess = onRequestNotifierAccess,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

private fun tilePixelSize(unit: Dp, colSpan: Int, rowSpan: Int): Pair<Dp, Dp> {
    val width = unit * colSpan + WidgetGridGap * (colSpan - 1)
    val height = unit * rowSpan + WidgetGridGap * (rowSpan - 1)
    return width to height
}

@Composable
private fun WidgetTileFace(
    kind: WidgetKind,
    clock: ClockFaceParts,
    battery: BatterySnapshot,
    storage: StorageSnapshot,
    notifierTray: NotifierTraySnapshot,
    notifierAccessGranted: Boolean,
    onRequestNotifierAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (kind) {
        WidgetKind.Notifier -> {
            NotifierTileFace(
                snapshot = notifierTray,
                accessGranted = notifierAccessGranted,
                onRequestAccess = onRequestNotifierAccess,
                modifier = modifier,
            )
            return
        }
        else -> Unit
    }
    val background = MetroTheme.colors.accent
    val content = MetroColors.tileContentColor(background)
    Box(
        modifier = modifier.background(background),
    ) {
        when (kind) {
            WidgetKind.Time -> TimeFace(
                clock = clock,
                contentColor = content,
                modifier = Modifier.fillMaxSize(),
            )
            WidgetKind.Battery -> BatteryFace(
                battery = battery,
                contentColor = content,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
            )
            WidgetKind.StorageSense -> StorageSenseFace(
                storage = storage,
                contentColor = content,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 28.dp),
            )
            WidgetKind.Notifier -> Unit
        }
        if (kind.showTitle) {
            BasicText(
                text = kind.title,
                style = TextStyle(
                    color = content,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontFamily = MetroTheme.fontFamily,
                    fontWeight = FontWeight.Normal,
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = TileTitlePaddingH, vertical = TileTitlePaddingV),
            )
        }
    }
}

/**
 * Wide digital clock — large thin digits, small AM/PM bottom-aligned to the left,
 * square colon dots. No weather / location / temperature.
 */
@Composable
private fun TimeFace(
    clock: ClockFaceParts,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        val timeSize = (maxHeight.value * 0.72f).coerceIn(72f, 120f).sp
        val periodSize = (timeSize.value * 0.22f).sp
        val colonDot = (timeSize.value * 0.08f).dp
        val timeStyle = TextStyle(
            color = contentColor,
            fontSize = timeSize,
            lineHeight = timeSize,
            fontFamily = MetroTheme.fontFamily,
            fontWeight = FontWeight.Thin,
        )
        val periodStyle = TextStyle(
            color = contentColor,
            fontSize = periodSize,
            lineHeight = periodSize,
            fontFamily = MetroTheme.fontFamily,
            fontWeight = FontWeight.Thin,
        )
        val density = LocalDensity.current

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
        ) {
            BasicText(
                text = clock.period,
                style = periodStyle,
                maxLines = 1,
                modifier = Modifier.padding(end = 8.dp, bottom = (timeSize.value * 0.30f).dp),
            )
            BasicText(
                text = clock.hour,
                style = timeStyle,
                maxLines = 1,
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .padding(bottom = (timeSize.value * 0.30f).dp)
                    .height(with(density) { timeSize.toDp() * 0.38f }),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(colonDot)
                        .background(contentColor),
                )
                Box(
                    modifier = Modifier
                        .size(colonDot)
                        .background(contentColor),
                )
            }
            BasicText(
                text = clock.minute,
                style = timeStyle,
                maxLines = 1,
            )
        }
    }
}

/** 1×1 Battery Saver–style face: vertical battery + digits (no `%`). */
@Composable
private fun BatteryFace(
    battery: BatterySnapshot,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val side = minOf(maxWidth, maxHeight)
        val digits = WidgetFormatters.batteryDigitsLabel(battery.percent)
        val glyphWidth = side * 0.26f
        val glyphHeight = side * 0.52f
        val gap = side * 0.12f
        // Leave room for 1–3 digits inside the remaining width.
        val textBudget = (maxWidth - glyphWidth - gap).coerceAtLeast(12.dp)
        val perDigit = textBudget / digits.length.coerceAtLeast(1)
        val fontSize = (perDigit.value * 1.08f)
            .coerceIn(20f, (side.value * 0.56f))
            .sp

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            VerticalBatteryGlyph(
                fraction = battery.fraction,
                color = contentColor,
                modifier = Modifier.size(width = glyphWidth, height = glyphHeight),
            )
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

@Composable
private fun StorageSenseFace(
    storage: StorageSnapshot,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
    ) {
        StorageVolumeBlock(
            title = storage.phone.label,
            free = WidgetFormatters.storageFreeLine(storage.phone),
            used = WidgetFormatters.storageUsedLine(storage.phone),
            contentColor = contentColor,
        )
        storage.sdCard?.let { sd ->
            Spacer(modifier = Modifier.height(10.dp))
            StorageVolumeBlock(
                title = sd.label,
                free = WidgetFormatters.storageFreeLine(sd),
                used = WidgetFormatters.storageUsedLine(sd),
                contentColor = contentColor,
            )
        }
    }
}

@Composable
private fun StorageVolumeBlock(
    title: String,
    free: String,
    used: String,
    contentColor: Color,
) {
    BasicText(
        text = title,
        style = TextStyle(
            color = contentColor.copy(alpha = 0.75f),
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontFamily = MetroTheme.fontFamily,
            fontWeight = FontWeight.Normal,
        ),
        maxLines = 1,
    )
    BasicText(
        text = free,
        style = TextStyle(
            color = contentColor,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            fontFamily = MetroTheme.fontFamily,
            fontWeight = FontWeight.Light,
        ),
        maxLines = 1,
    )
    BasicText(
        text = used,
        style = TextStyle(
            color = contentColor.copy(alpha = 0.85f),
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontFamily = MetroTheme.fontFamily,
            fontWeight = FontWeight.Normal,
        ),
        maxLines = 1,
    )
}

@Composable
private fun VerticalBatteryGlyph(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = size.width * 0.12f
        val capHeight = size.height * 0.08f
        val capWidth = size.width * 0.42f
        val bodyTop = capHeight + size.height * 0.04f
        val bodyLeft = 0f
        val bodyRight = size.width
        val bodyBottom = size.height
        val bodyHeight = bodyBottom - bodyTop

        // Cap on top.
        drawRect(
            color = color,
            topLeft = Offset((size.width - capWidth) / 2f, 0f),
            size = Size(capWidth, capHeight),
        )

        // Outline.
        drawRect(
            color = color,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyRight - bodyLeft, bodyHeight),
            style = Stroke(width = stroke),
        )

        // Fill from the bottom up.
        val inset = stroke * 1.4f
        val fillTrackHeight = bodyHeight - inset * 2f
        val fillHeight = fillTrackHeight * fraction.coerceIn(0f, 1f)
        if (fillHeight > 0f) {
            drawRect(
                color = color,
                topLeft = Offset(
                    bodyLeft + inset,
                    bodyBottom - inset - fillHeight,
                ),
                size = Size(bodyRight - bodyLeft - inset * 2f, fillHeight),
            )
        }
    }
}
