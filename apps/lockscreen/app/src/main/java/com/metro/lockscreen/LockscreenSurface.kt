package com.metro.lockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.core.content.ContextCompat
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** Softened chrome on AMOLED glance — less harsh than pure white on black. */
private val GlanceContentColor = Color(0xFFCCCCCC)

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
 * - Release at/above threshold, or a decisive upward fling → [onExitStarted] immediately
 *   (so the host can drop WM touch ownership), animate fully off-screen, then
 *   [onExitFinished] with [LockscreenExitReason.SwipeCommit].
 * - [exitController] can request the same slide-off for biometric unlock
 *   ([LockscreenExitReason.BiometricUnlock]).
 *
 * Snap-back bounce is pure vertical translation. Spring overshoot past rest is mirrored
 * upward ([LockscreenLogic.bounceTranslationY]) so the fill jumps off the top and the
 * gap opens at the bottom — never sinks under the status bar, never scale/squash.
 *
 * Never arms SystemUI mid-drag — swipe unlock is only requested after a committed slide-off.
 *
 * The outer hit target stays full-screen while only the inner fill translates. Without an
 * immediate [onExitStarted] → FLAG_NOT_TOUCHABLE hand-off, that empty outer layer eats taps
 * over Start after unlock.
 */
@Composable
fun LockscreenSurface(
    onExitFinished: (LockscreenExitReason) -> Unit,
    modifier: Modifier = Modifier,
    mode: LockscreenPresentationMode = LockscreenPresentationMode.Lock,
    fillColor: Color = MetroTheme.colors.accent,
    /** System status-bar / cutout band height in px — tray icons are centered in this region. */
    topInsetPx: Int = 0,
    exitController: LockscreenExitController? = null,
    /** Fired at the start of a committed exit so the host can stop eating touches. */
    onExitStarted: (LockscreenExitReason) -> Unit = {},
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
            val status = LockscreenNotificationStore.statusFor(pkg)
            if (!LockscreenQuickStatusLogic.shouldShowQuickStatus(status.count)) {
                return@mapIndexedNotNull null
            }
            LockscreenQuickStatusItem(
                slotIndex = slotIndex,
                packageName = pkg,
                icon = icon,
                count = status.count,
                flipGeneration = status.flipGeneration,
            )
        }
    }

    DisposableEffect(Unit) {
        val mainHandler = Handler(Looper.getMainLooper())
        val listener: () -> Unit = {
            // NotificationListener delivers on a binder thread — Compose state must bump on main.
            if (Looper.myLooper() == Looper.getMainLooper()) {
                quickStatusTick++
            } else {
                mainHandler.post { quickStatusTick++ }
            }
        }
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

    // Handler + TIME_TICK — not Compose delay. Overlay recomposers pause without frames
    // (AOD / doze / idle lock), and a thrown calendar query killed the old LaunchedEffect loop
    // until the overlay was torn down on the next wake.
    DisposableEffect(calendar) {
        val appContext = context.applicationContext
        val handler = Handler(Looper.getMainLooper())
        fun refreshChromeLabels() {
            try {
                labels = calendar.loadChromeLabels(ZonedDateTime.now())
            } catch (t: Throwable) {
                Log.w(TAG, "chrome labels refresh failed", t)
            }
        }
        val tickRunnable = object : Runnable {
            override fun run() {
                refreshChromeLabels()
                val wait = LockscreenChromeLogic.millisUntilNextMinute(System.currentTimeMillis())
                handler.postDelayed(this, wait)
            }
        }
        fun rescheduleTick() {
            handler.removeCallbacks(tickRunnable)
            val wait = LockscreenChromeLogic.millisUntilNextMinute(System.currentTimeMillis())
            handler.postDelayed(tickRunnable, wait)
        }
        val timeReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                refreshChromeLabels()
                // Realign the Handler schedule after TIME_TICK / wall-clock / zone changes.
                rescheduleTick()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(
            appContext,
            timeReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        handler.post(tickRunnable)
        onDispose {
            handler.removeCallbacks(tickRunnable)
            runCatching { appContext.unregisterReceiver(timeReceiver) }
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

    val contentColor = if (isGlance) GlanceContentColor else fill.contentColor
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

    fun commitExit(reason: LockscreenExitReason) {
        if (unlockCommitted) return
        unlockCommitted = true
        phase.value = LockscreenLogic.SwipePhase.Committing
        // Drop WM touch ownership before the fill slides away — the outer Box stays
        // full-screen and would otherwise hijack taps over the app underneath.
        onExitStarted(reason)
        cancelOffsetJob()
        offsetJob = scope.launch {
            val measured = size.height.toFloat()
            val fallback = context.resources.displayMetrics.heightPixels.toFloat()
            val offscreen = -(measured.takeIf { it > 1f } ?: fallback).coerceAtLeast(1f)
            offsetAnim.snapTo(rawOffsetY)
            try {
                offsetAnim.animateTo(
                    targetValue = offscreen,
                    // Ease-out so the fill decelerates off-screen instead of snapping away.
                    animationSpec = tween(
                        durationMillis = COMMIT_EXIT_MS,
                        easing = CommitExitEasing,
                    ),
                ) {
                    rawOffsetY = value
                }
            } finally {
                // Always finish — cancelled animations must not leave a half-slid fill.
                rawOffsetY = offscreen
                phase.value = LockscreenLogic.SwipePhase.HandedOff
                onExitFinished(reason)
            }
        }
    }

    DisposableEffect(exitController, isGlance) {
        if (exitController == null || isGlance) {
            return@DisposableEffect onDispose { }
        }
        val playExit: () -> Unit = {
            commitExit(LockscreenExitReason.BiometricUnlock)
        }
        exitController.bind(playExit)
        onDispose { exitController.unbind(playExit) }
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
                                    LockscreenLogic.ReleaseAction.Commit ->
                                        commitExit(LockscreenExitReason.SwipeCommit)
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
                flipOnNewNotification = isGlance,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .metroNavBarPadding()
                    .padding(bottom = 20.dp),
            )
        }
    }
}

private const val TAG = "LockscreenSurface"

/** Commit slide-off duration — slightly longer than the old 280ms snap for a WP-like ease. */
private const val COMMIT_EXIT_MS = 420

/** Decelerate into off-screen rest (ease-out). */
private val CommitExitEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
