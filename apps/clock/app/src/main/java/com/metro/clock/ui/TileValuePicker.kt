package com.metro.clock.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal val TileGap = 8.dp
internal val TileMinSize = 56.dp

private val FlipEntranceDelayMs = MetroTransitions.PagePivotLoadMs.toLong() + 120L
private val CenterFirstStaggerMs = 40L
private val TileFontSize = 48.sp
/** Soft fling → tile steps; capped so a flick cannot empty the viewport. */
private const val FlingTilesPerPx = 0.00035f
private const val MaxFlingTiles = 4

/**
 * One column in the WP tile time / duration picker: collapsed accent tile that flips in,
 * or an expanded vertical value scroller.
 */
@Composable
internal fun TimeFieldColumn(
    label: String,
    flipIndex: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    values: List<Int>,
    selectedIndex: Int,
    format: (Int) -> String,
    onSelectedIndex: (Int) -> Unit,
    looping: Boolean,
    expandBelow: Boolean,
    tileSize: Dp,
    visibleRows: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(MetroTransitions.PagePivotLoadMs)),
            exit = fadeOut(tween(MetroTransitions.PagePivotLoadMs)),
            modifier = Modifier.fillMaxSize(),
        ) {
            TimeValueScroller(
                values = values,
                selectedIndex = selectedIndex,
                format = format,
                onSelectedIndex = onSelectedIndex,
                looping = looping,
                expandBelow = expandBelow,
                tileSize = tileSize,
                visibleRows = visibleRows,
                onSelectedTap = onToggle,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (!expanded) {
            CenterFlipTile(
                flipIndex = flipIndex,
                label = label,
                tileSize = tileSize,
                onClick = onToggle,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun CenterFlipTile(
    flipIndex: Int,
    label: String,
    tileSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotationX = remember { Animatable(90f) }
    val density = LocalDensity.current
    LaunchedEffect(flipIndex) {
        val stagger = when (flipIndex) {
            1 -> 0L
            else -> CenterFirstStaggerMs
        }
        rotationX.snapTo(90f)
        delay(FlipEntranceDelayMs + stagger)
        rotationX.animateTo(
            targetValue = 0f,
            animationSpec = MetroTransitions.jumpListFlipTween(),
        )
    }
    Box(
        modifier = modifier
            .size(tileSize)
            .graphicsLayer {
                this.rotationX = rotationX.value
                transformOrigin = TransformOrigin(0.5f, 0.5f)
                cameraDistance = 16f * density.density
            }
            .background(MetroTheme.colors.accent, RectangleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(start = 8.dp, bottom = 6.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        TileGlyph(text = label, color = Color.White)
    }
}

@Composable
private fun TileGlyph(text: String, color: Color) {
    BasicText(
        text = text,
        style = MetroTextStyle.ListItemTitle
            .toTextStyle(MetroTheme.fontFamily)
            .copy(
                color = color,
                fontWeight = FontWeight.SemiBold,
                fontSize = TileFontSize,
            ),
    )
}

/**
 * Vertical value list of square tiles.
 *
 * [expandBelow] (AM/PM): fixed list order, selection seat at the collapsed tile.
 * Expansion viewport grows downward only (content above the seat is clipped). Dragging
 * scrolls the list so the lower option pushes up into the seat — no label swapping.
 *
 * Other fields: centered looping window with capped fling + spring lock.
 */
@Composable
private fun TimeValueScroller(
    values: List<Int>,
    selectedIndex: Int,
    format: (Int) -> String,
    onSelectedIndex: (Int) -> Unit,
    looping: Boolean,
    expandBelow: Boolean,
    tileSize: Dp,
    visibleRows: Int,
    onSelectedTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tilePx = with(density) { tileSize.toPx() }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val settle = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(selectedIndex) }
    // Absolute scroll for expandBelow: item i top = i * tilePx + scroll; seat at 0.
    var belowScroll by remember { mutableFloatStateOf(-selectedIndex * tilePx) }
    val belowSettle = remember { Animatable(-selectedIndex * tilePx) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedIndex) {
        if (!dragging && index != selectedIndex) {
            index = selectedIndex
            dragOffset = 0f
            settle.snapTo(0f)
            if (expandBelow) {
                val target = -selectedIndex * tilePx
                belowSettle.snapTo(belowScroll)
                belowSettle.animateTo(
                    target,
                    spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium),
                )
                belowScroll = belowSettle.value
            }
        }
    }

    val visualOffset = if (dragging) dragOffset else settle.value
    val belowVisual = if (dragging) belowScroll else belowSettle.value

    fun clampOrLoop(raw: Int): Int = when {
        looping -> ((raw % values.size) + values.size) % values.size
        else -> raw.coerceIn(0, values.lastIndex)
    }

    fun foldDragOffset() {
        if (looping) {
            while (dragOffset <= -tilePx) {
                dragOffset += tilePx
                index = clampOrLoop(index + 1)
            }
            while (dragOffset >= tilePx) {
                dragOffset -= tilePx
                index = clampOrLoop(index - 1)
            }
        } else {
            while (dragOffset <= -tilePx && index < values.lastIndex) {
                dragOffset += tilePx
                index += 1
            }
            while (dragOffset >= tilePx && index > 0) {
                dragOffset -= tilePx
                index -= 1
            }
            if (index == 0 && dragOffset > 0f) dragOffset = 0f
            if (index == values.lastIndex && dragOffset < 0f) dragOffset = 0f
        }
    }

    fun lockBelowList(velocityPx: Float) {
        scope.launch {
            val minScroll = -((values.lastIndex).coerceAtLeast(0) * tilePx)
            val current = belowScroll
            // Soft velocity nudge, then snap to nearest index — 1-step feel for 2 tiles.
            val projected = (current + velocityPx * 0.08f).coerceIn(minScroll, 0f)
            var next = (-projected / tilePx).roundToInt().coerceIn(0, values.lastIndex)
            if (next == index && kotlin.math.abs(velocityPx) > 400f) {
                next = if (velocityPx < 0f) {
                    (index + 1).coerceAtMost(values.lastIndex)
                } else {
                    (index - 1).coerceAtLeast(0)
                }
            }
            index = next
            onSelectedIndex(next)
            dragging = false
            belowSettle.snapTo(current)
            belowSettle.animateTo(
                targetValue = -next * tilePx,
                animationSpec = spring(
                    dampingRatio = 0.9f,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
            belowScroll = belowSettle.value
        }
    }

    fun lockToNearest(velocityPx: Float) {
        scope.launch {
            val fromDrag = (-dragOffset / tilePx).roundToInt()
            val flingTiles = (-velocityPx * FlingTilesPerPx)
                .roundToInt()
                .coerceIn(-MaxFlingTiles, MaxFlingTiles)
            val next = clampOrLoop(index + fromDrag + flingTiles)
            val residual = dragOffset + (next - index) * tilePx
            index = next
            onSelectedIndex(next)
            dragOffset = 0f
            dragging = false
            settle.snapTo(residual)
            settle.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }

    val dragState = rememberDraggableState { delta ->
        if (expandBelow) {
            val minScroll = -((values.lastIndex).coerceAtLeast(0) * tilePx)
            belowScroll = (belowScroll + delta).coerceIn(minScroll, 0f)
        } else {
            dragOffset += delta
            foldDragOffset()
        }
    }

    Box(
        modifier = modifier
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStarted = {
                    dragging = true
                    if (expandBelow) {
                        scope.launch { belowSettle.stop() }
                        belowScroll = belowSettle.value
                    } else {
                        dragOffset = settle.value
                        scope.launch { settle.stop() }
                    }
                },
                onDragStopped = { velocity ->
                    if (expandBelow) lockBelowList(velocity) else lockToNearest(velocity)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val outline = MetroTheme.colors.primaryText.copy(alpha = 0.55f)
        val showAccent = !dragging
        val half = visibleRows / 2

        if (expandBelow) {
            // Clip box: top = seat top, grows downward only. Fixed AM→PM order scrolls
            // through the seat so the lower option pushes up — labels never swap.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -tileSize / 2)
                    .width(tileSize)
                    .height(tileSize * values.size.coerceAtLeast(1))
                    .graphicsLayer { clip = true },
            ) {
                values.forEachIndexed { i, value ->
                    val atSeat = kotlin.math.abs(belowVisual + i * tilePx) < tilePx * 0.5f
                    val isSelected = atSeat && showAccent
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = (i * tilePx + belowVisual).roundToInt(),
                                )
                            }
                            .size(tileSize)
                            .then(
                                if (isSelected) {
                                    Modifier.background(MetroTheme.colors.accent, RectangleShape)
                                } else {
                                    Modifier.border(2.dp, outline, RectangleShape)
                                },
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (atSeat) {
                                        onSelectedTap()
                                    } else {
                                        scope.launch {
                                            index = i
                                            onSelectedIndex(i)
                                            belowSettle.snapTo(belowScroll)
                                            belowSettle.animateTo(
                                                -i * tilePx,
                                                spring(
                                                    dampingRatio = 0.9f,
                                                    stiffness = Spring.StiffnessMedium,
                                                ),
                                            )
                                            belowScroll = belowSettle.value
                                        }
                                    }
                                },
                            )
                            .padding(start = 8.dp, bottom = 6.dp),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        TileGlyph(
                            text = format(value),
                            color = if (isSelected) {
                                Color.White
                            } else {
                                MetroTheme.colors.primaryText
                            },
                        )
                    }
                }
            }
        } else {
            val slots = (-half..half).map { offset ->
                offset to clampOrLoop(index + offset)
            }
            Box(
                modifier = Modifier
                    .width(tileSize)
                    .height(tileSize * visibleRows)
                    .graphicsLayer { clip = true },
            ) {
                slots.forEachIndexed { slotIndex, (offset, valueIndex) ->
                    val isSelected = offset == 0 && showAccent
                    val y = slotIndex * tilePx + visualOffset
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, y.roundToInt()) }
                            .size(tileSize)
                            .then(
                                if (isSelected) {
                                    Modifier.background(MetroTheme.colors.accent, RectangleShape)
                                } else {
                                    Modifier.border(2.dp, outline, RectangleShape)
                                },
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (offset == 0) {
                                        onSelectedTap()
                                    } else {
                                        index = valueIndex
                                        onSelectedIndex(valueIndex)
                                        scope.launch {
                                            dragOffset = 0f
                                            settle.snapTo(0f)
                                        }
                                    }
                                },
                            )
                            .padding(start = 8.dp, bottom = 6.dp),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        TileGlyph(
                            text = format(values[valueIndex]),
                            color = if (isSelected) {
                                Color.White
                            } else {
                                MetroTheme.colors.primaryText
                            },
                        )
                    }
                }
            }
        }
    }
}
