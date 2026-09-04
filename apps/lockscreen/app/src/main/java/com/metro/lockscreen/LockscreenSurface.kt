package com.metro.lockscreen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** How the lock overlay is presented — full lock fill or AMOLED glance over system AOD. */
enum class LockscreenPresentationMode {
    /** Accent / photo / Bing fill with swipe-up unlock and quick-status row. */
    Lock,

    /** Pure black fill over system AOD — chrome + quick-status icons, no wallpaper or status tray. */
    Glance,
}

/**
 * Full-bleed lock fill with WP8.1 chrome (time / day / date / next event) and an explicit
 * swipe session:
 * - Drag tracks the finger upward only (offset updated synchronously — no async snap race).
 * - Release below threshold (and without a qualifying fling) → spring bounce back.
 * - Release at/above threshold, or a decisive upward fling → animate fully off-screen,
 *   then [onUnlockCommitted] once (also if the slide-off animation is cancelled).
 *
 * Snap-back bounce is pure vertical translation. Spring overshoot past rest is mirrored
 * upward ([LockscreenLogic.bounceTranslationY]) so the fill jumps off the top and the
 * gap opens at the bottom — never sinks under the status bar, never scale/squash.
 *
 * Never arms SystemUI mid-drag — unlock is only requested after a committed slide-off.
 */
@Composable
fun LockscreenSurface(
    onUnlockCommitted: () -> Unit,
    modifier: Modifier = Modifier,
    mode: LockscreenPresentationMode = LockscreenPresentationMode.Lock,
    fillColor: Color = MetroTheme.colors.accent,
    /** System status-bar / cutout band height in px — tray icons are centered in this region. */
    topInsetPx: Int = 0,
) {
    val isGlance = mode == LockscreenPresentationMode.Glance
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // Raw Y used for graphicsLayer. Updated synchronously during drag so release
    // decisions never race a lagging Animatable; Animatable only drives settle/commit.
    var rawOffsetY by remember { mutableFloatStateOf(0f) }
    val offsetAnim = remember { Animatable(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    // Holders (not keys) so pointerInput is not restarted when phase / threshold change.
    val phase = remember { mutableStateOf(LockscreenLogic.SwipePhase.Idle) }
    val thresholdHolder = remember { mutableFloatStateOf(1f) }
    val flingVelocityHolder = remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var offsetJob by remember { mutableStateOf<Job?>(null) }
    val velocityTracker = remember { VelocityTracker() }
    var unlockCommitted by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember(context) { LockscreenPreferences(context) }
    val calendar = remember(context) { LockscreenCalendarRepository(context) }
    var labels by remember {
        mutableStateOf(calendar.loadChromeLabels())
    }
    var quickStatusTick by remember { mutableIntStateOf(0) }
    val quickStatusIconPx = with(density) { 22.dp.roundToPx().coerceAtLeast(1) }
    val quickStatusItems = remember(quickStatusTick, quickStatusIconPx) {
        prefs.quickStatusSlots().mapIndexedNotNull { slotIndex, packageName ->
            val pkg = packageName ?: return@mapIndexedNotNull null
            val icon = resolveQuickStatusIcon(context, pkg, quickStatusIconPx)
            if (!icon.hasIcon) return@mapIndexedNotNull null
            val count = LockscreenNotificationStore.countFor(pkg)
            if (!LockscreenQuickStatusLogic.shouldShowQuickStatus(count)) return@mapIndexedNotNull null
            LockscreenQuickStatusItem(
                slotIndex = slotIndex,
                packageName = pkg,
                icon = icon,
                count = count,
            )
        }
    }

    DisposableEffect(Unit) {
        val listener: () -> Unit = { quickStatusTick++ }
        LockscreenNotificationStore.addListener(listener)
        onDispose { LockscreenNotificationStore.removeListener(listener) }
    }
    var fill by remember {
        mutableStateOf(
            LockscreenFill(
                mode = LockscreenBackgroundMode.Accent,
                accentColor = fillColor,
                bitmap = null,
            ),
        )
    }

    LaunchedEffect(fillColor, isGlance) {
        fill = if (isGlance) {
            LockscreenFill(
                mode = LockscreenBackgroundMode.Accent,
                accentColor = Color.Black,
                bitmap = null,
            )
        } else {
            LockscreenBackgroundResolver.resolve(context, fillColor)
        }
    }

    LaunchedEffect(calendar) {
        while (isActive) {
            labels = calendar.loadChromeLabels(ZonedDateTime.now())
            val wait = LockscreenChromeLogic.millisUntilNextMinute(System.currentTimeMillis())
            delay(wait)
        }
    }

    val thresholdPx = remember(size, density) {
        LockscreenLogic.unlockThresholdPx(
            screenHeightPx = size.height.toFloat().coerceAtLeast(1f),
            density = density.density,
        )
    }
    thresholdHolder.floatValue = thresholdPx
    flingVelocityHolder.floatValue = LockscreenLogic.flingVelocityPx(density.density)

    val contentColor = if (isGlance) Color.White else fill.contentColor
    val solidFill = if (isGlance) Color.Black else fill.accentColor

    fun cancelOffsetJob() {
        offsetJob?.cancel()
        offsetJob = null
    }

    fun snapBack() {
        phase.value = LockscreenLogic.SwipePhase.SettlingBack
        cancelOffsetJob()
        offsetJob = scope.launch {
            offsetAnim.snapTo(rawOffsetY)
            offsetAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) {
                rawOffsetY = value
            }
            // Interrupted by a new drag → phase is no longer SettlingBack; leave it alone.
            if (phase.value == LockscreenLogic.SwipePhase.SettlingBack) {
                dragAccum = 0f
                rawOffsetY = 0f
                phase.value = LockscreenLogic.SwipePhase.Idle
            }
        }
    }

    fun commitUnlock() {
        if (unlockCommitted) return
        phase.value = LockscreenLogic.SwipePhase.Committing
        cancelOffsetJob()
        offsetJob = scope.launch {
            val offscreen = -(size.height.toFloat().coerceAtLeast(1f))
            offsetAnim.snapTo(rawOffsetY)
            try {
                offsetAnim.animateTo(
                    targetValue = offscreen,
                    animationSpec = tween(durationMillis = 220),
                ) {
                    rawOffsetY = value
                }
            } finally {
                // Always hand off — cancelled animations must not leave a half-slid fill.
                if (!unlockCommitted) {
                    unlockCommitted = true
                    rawOffsetY = offscreen
                    phase.value = LockscreenLogic.SwipePhase.HandedOff
                    onUnlockCommitted()
                }
            }
        }
    }

    fun beginDrag() {
        phase.value = LockscreenLogic.SwipePhase.Dragging
        cancelOffsetJob()
        // Take over from the live visual offset (may be mid-bounce).
        dragAccum = LockscreenLogic.clampDragOffsetY(rawOffsetY)
        rawOffsetY = dragAccum
        velocityTracker.resetTracking()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .then(
                if (isGlance) {
                    Modifier
                } else {
                    // Unit key — never restart the detector mid-gesture (threshold lives in holders).
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                if (!LockscreenLogic.phaseAllowsDrag(phase.value)) {
                                    return@detectVerticalDragGestures
                                }
                                beginDrag()
                            },
                            onVerticalDrag = { change, dragAmount ->
                                if (phase.value != LockscreenLogic.SwipePhase.Dragging) {
                                    return@detectVerticalDragGestures
                                }
                                change.consume()
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                val next = LockscreenLogic.clampDragOffsetY(dragAccum + dragAmount)
                                dragAccum = next
                                // Synchronous — release must see the same value the finger moved.
                                rawOffsetY = next
                            },
                            onDragCancel = {
                                if (phase.value == LockscreenLogic.SwipePhase.Dragging) {
                                    velocityTracker.resetTracking()
                                    snapBack()
                                }
                            },
                            onDragEnd = {
                                if (phase.value != LockscreenLogic.SwipePhase.Dragging) {
                                    return@detectVerticalDragGestures
                                }
                                val velocityY = velocityTracker.calculateVelocity().y
                                velocityTracker.resetTracking()
                                when (
                                    LockscreenLogic.decideRelease(
                                        offsetY = dragAccum,
                                        thresholdPx = thresholdHolder.floatValue,
                                        velocityY = velocityY,
                                        flingVelocityPx = flingVelocityHolder.floatValue,
                                    )
                                ) {
                                    LockscreenLogic.ReleaseAction.Commit -> commitUnlock()
                                    LockscreenLogic.ReleaseAction.SnapBack -> snapBack()
                                }
                            },
                        )
                    }
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (!isGlance) {
                        // Translate only; mirror spring overshoot upward (gap at bottom).
                        translationY = LockscreenLogic.bounceTranslationY(rawOffsetY)
                    }
                }
                .background(solidFill),
        ) {
            if (!isGlance) {
                fill.bitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            // Transparent Metro tray icons over the fill (no bar chrome) — lock only, not glance.
            if (!isGlance) {
                LockscreenStatusBar(
                    color = contentColor,
                    topInsetPx = topInsetPx,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
            LockscreenChrome(
                labels = labels,
                contentColor = contentColor,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .metroNavBarPadding()
                    .padding(bottom = if (quickStatusItems.isNotEmpty()) 96.dp else 56.dp),
            )
            LockscreenQuickStatusBar(
                items = quickStatusItems,
                contentColor = contentColor,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .metroNavBarPadding()
                    .padding(bottom = 20.dp),
            )
        }
    }
}
