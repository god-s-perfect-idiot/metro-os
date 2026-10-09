package com.metro.statusbar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.metro.statusbar.TrayIconFlags
import com.metro.statusbar.TrayLayout
import com.metro.statusbar.TrayLayoutSlot
import com.metro.statusbar.TraySpec
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import kotlin.math.abs
import kotlin.math.roundToInt

/** Sharp Metro drag handle: cone tip + square grip box. */
private val ThumbBoxWidth = 28.dp
private val ThumbBoxHeight = 26.dp
private val ThumbConeHeight = 8.dp
private val ThumbConeBaseWidth = 12.dp
private val ThumbTotalHeight = ThumbConeHeight + ThumbBoxHeight
private val ThumbHitWidth = 44.dp
private val ThumbGap = 2.dp

/** Rounded delete control above spacer slots. */
private val DeleteButtonSize = 36.dp
private val DeleteGap = 8.dp
/**
 * Configure-page tray arrange panel. The strip is the same [TrayPreview] / [StatusTray]
 * renderer as the live overlay (1:1). Accent thumbs sit under each slot; drag shifts the
 * item one neighbor at a time as the finger crosses that neighbor's center. Spacers show a
 * rounded delete button above the tray to remove them.
 */
@Composable
fun TrayLayoutEditor(
    slots: List<TrayLayoutSlot>,
    iconFlags: TrayIconFlags,
    clockText: String,
    onSlotsChange: (List<TrayLayoutSlot>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MetroTheme.colors.accent
    val foreground = MetroTheme.colors.primaryText
    val density = LocalDensity.current
    val deleteButtonSizePx = with(density) { DeleteButtonSize.toPx() }
    val deleteGapPx = with(density) { DeleteGap.toPx() }
    val trayHeightPx = with(density) { TraySpec.TRAY_HEIGHT_DP.dp.toPx() }
    val thumbGapPx = with(density) { ThumbGap.toPx() }
    val thumbHitWidthPx = with(density) { ThumbHitWidth.toPx() }
    val trayTopPx = deleteButtonSizePx + deleteGapPx
    val thumbTopPx = trayTopPx + trayHeightPx + thumbGapPx

    var slotCenters by remember { mutableStateOf<List<Float>>(emptyList()) }
    var dragSlotId by remember { mutableStateOf<String?>(null) }
    var dragFingerX by remember { mutableFloatStateOf(0f) }
    var editorWidthPx by remember { mutableIntStateOf(0) }
    // After a live swap, freeze until StatusTray republishes centers so we don't
    // cascade multiple shifts on stale geometry.
    var centersFreeze by remember { mutableStateOf<List<Float>?>(null) }

    val slotsRef = rememberUpdatedState(slots)
    val onChangeRef = rememberUpdatedState(onSlotsChange)
    val centersRef = rememberUpdatedState(slotCenters)

    fun clampOverlayX(centerX: Float, overlayWidthPx: Float): Float {
        val maxX = (editorWidthPx - overlayWidthPx).coerceAtLeast(0f)
        return (centerX - overlayWidthPx / 2f).coerceIn(0f, maxX)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(
                DeleteButtonSize + DeleteGap +
                    TraySpec.TRAY_HEIGHT_DP.dp + ThumbGap + ThumbTotalHeight,
            )
            .onSizeChanged { editorWidthPx = it.width },
    ) {
        TrayPreview(
            flags = iconFlags,
            clockText = clockText,
            layout = slots,
            onSlotCentersChanged = { next ->
                if (slotCenters != next) {
                    slotCenters = next
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, trayTopPx.roundToInt()) }
                .align(Alignment.TopCenter),
        )

        if (slotCenters.size == slots.size) {
            slots.forEachIndexed { index, slot ->
                val homeX = slotCenters[index]
                val isDragging = dragSlotId == slot.id
                val thumbCenterX = if (isDragging) dragFingerX else homeX

                if (slot is TrayLayoutSlot.Spacer) {
                    Box(
                        modifier = Modifier
                            .zIndex(if (isDragging) 2f else 1f)
                            .offset {
                                IntOffset(
                                    x = clampOverlayX(thumbCenterX, deleteButtonSizePx)
                                        .roundToInt(),
                                    y = 0,
                                )
                            }
                            .size(DeleteButtonSize)
                            .metroClickable {
                                onChangeRef.value(
                                    TrayLayout.removeSpacer(slotsRef.value, slot.id),
                                )
                            }
                            .semantics { contentDescription = "Remove spacer" },
                        contentAlignment = Alignment.Center,
                    ) {
                        SpacerDeleteButton()
                    }
                }

                val thumbDesc = when (slot) {
                    is TrayLayoutSlot.Icon -> "Move ${slot.kind.name}"
                    is TrayLayoutSlot.Spacer -> "Move spacer"
                }
                Box(
                    modifier = Modifier
                        .zIndex(if (isDragging) 2f else 0f)
                        .offset {
                            IntOffset(
                                x = clampOverlayX(thumbCenterX, thumbHitWidthPx).roundToInt(),
                                y = thumbTopPx.roundToInt(),
                            )
                        }
                        .size(width = ThumbHitWidth, height = ThumbTotalHeight)
                        .semantics { contentDescription = thumbDesc }
                        .pointerInput(slot.id) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startSlots = slotsRef.value
                                val startIndex =
                                    startSlots.indexOfFirst { it.id == slot.id }
                                if (startIndex < 0) return@awaitEachGesture
                                val startCenters = centersRef.value
                                if (startIndex !in startCenters.indices) {
                                    return@awaitEachGesture
                                }
                                val startCenter = startCenters[startIndex]
                                dragSlotId = slot.id
                                dragFingerX = startCenter
                                centersFreeze = null
                                drag(down.id) { change ->
                                    val dx = change.positionChange().x
                                    dragFingerX += dx
                                    maybeShiftOneSlot(
                                        slotId = slot.id,
                                        fingerX = dragFingerX,
                                        slots = slotsRef.value,
                                        centers = centersRef.value,
                                        centersFreeze = centersFreeze,
                                        onShift = { nextSlots, freeze ->
                                            centersFreeze = freeze
                                            onChangeRef.value(nextSlots)
                                        },
                                    )
                                    change.consume()
                                }
                                dragSlotId = null
                                centersFreeze = null
                            }
                        },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    DragThumbGlyph(
                        fill = accent,
                        outline = if (isDragging) foreground else accent,
                        grip = Color.White,
                    )
                }
            }
        }
    }
}

/** Circle + toolkit filled trash glyph for removing a spacer. */
@Composable
private fun SpacerDeleteButton(
    modifier: Modifier = Modifier,
) {
    MetroSystemIcon(
        type = MetroSystemIconType.Delete,
        iconSize = DeleteButtonSize,
        color = Color.White,
        showCircle = true,
        modifier = modifier,
    )
}

/** Accent square with a sharp upward cone and three grip lines. */
@Composable
private fun DragThumbGlyph(
    fill: Color,
    outline: Color,
    grip: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.size(
            width = ThumbBoxWidth,
            height = ThumbTotalHeight,
        ),
    ) {
        val stroke = 1.dp.toPx()
        val coneH = ThumbConeHeight.toPx()
        val coneHalf = ThumbConeBaseWidth.toPx() / 2f
        val boxTop = coneH
        val boxH = size.height - coneH
        val cx = size.width / 2f

        val cone = Path().apply {
            moveTo(cx, 0f)
            lineTo(cx + coneHalf, coneH)
            lineTo(cx - coneHalf, coneH)
            close()
        }
        drawPath(cone, color = fill)
        drawPath(cone, color = outline, style = Stroke(width = stroke))

        drawRect(
            color = fill,
            topLeft = Offset(0f, boxTop),
            size = Size(size.width, boxH),
        )
        drawRect(
            color = outline,
            topLeft = Offset(0f, boxTop),
            size = Size(size.width, boxH),
            style = Stroke(width = stroke),
        )

        // Three horizontal grip lines — Metro drag affordance.
        val lineInset = size.width * 0.22f
        val lineStroke = 1.5.dp.toPx()
        val gripTop = boxTop + boxH * 0.28f
        val gripSpan = boxH * 0.44f
        repeat(3) { i ->
            val y = gripTop + gripSpan * (i / 2f)
            drawLine(
                color = grip,
                start = Offset(lineInset, y),
                end = Offset(size.width - lineInset, y),
                strokeWidth = lineStroke,
                cap = StrokeCap.Butt,
            )
        }
    }
}

/**
 * When the dragged finger crosses an immediate neighbor's center, move exactly one slot
 * and freeze until centers republish (prevents multi-jump on stale layout).
 */
private fun maybeShiftOneSlot(
    slotId: String,
    fingerX: Float,
    slots: List<TrayLayoutSlot>,
    centers: List<Float>,
    centersFreeze: List<Float>?,
    onShift: (nextSlots: List<TrayLayoutSlot>, freeze: List<Float>) -> Unit,
) {
    if (centers.size != slots.size || centers.isEmpty()) return
    if (centersFreeze != null && centersFreeze == centers) return
    val from = slots.indexOfFirst { it.id == slotId }
    if (from < 0) return
    val target = when {
        from < slots.lastIndex && fingerX > centers[from + 1] -> from + 1
        from > 0 && fingerX < centers[from - 1] -> from - 1
        else -> return
    }
    onShift(TrayLayout.move(slots, from, target), centers)
}
