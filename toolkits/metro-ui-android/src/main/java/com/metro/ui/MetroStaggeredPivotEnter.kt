package com.metro.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Staggered Disco continuum enter/exit for list hubs — same motion as Start tiles /
 * [MetroAppLaunchPivot] (enter: outer `rotateY` 70° + inner slide; exit: tilt-back + slide).
 *
 * Delay is [metroListPivotEnterDelayMs] for [staggerIndex] on enter, and a tighter
 * [MetroTransitions.ListPivotExitStaggerMs] cascade on exit (top-down):
 * - **0** — app name / page titles (starts immediately)
 * - **1…n** — list rows
 *
 * Prefer wiring through [MetroListPivotController] so LazyColumn scroll-ins past their
 * slot time appear at rest (no invisible wait / blink).
 */
@Composable
fun MetroStaggeredPivotEnter(
    staggerIndex: Int,
    modifier: Modifier = Modifier,
    loadKey: Any? = Unit,
    exiting: Boolean = false,
    skipEnter: Boolean = false,
    /**
     * Remaining enter delay override (e.g. from [MetroListPivotController.enterDelayMsFor]).
     * Ignored while [exiting] — exit always uses the exit stagger table.
     */
    enterDelayMs: Long? = null,
    staggerMs: Int = MetroTransitions.ListPivotStaggerMs,
    exitStaggerMs: Int = MetroTransitions.ListPivotExitStaggerMs,
    onExitComplete: (() -> Unit)? = null,
    onEnterComplete: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val delayMs = if (exiting) {
        metroListPivotEnterDelayMs(
            staggerIndex = staggerIndex,
            staggerMs = exitStaggerMs,
            maxIndex = MetroTransitions.ListPivotExitStaggerMaxIndex,
        )
    } else {
        enterDelayMs ?: metroListPivotEnterDelayMs(staggerIndex, staggerMs)
    }
    MetroAppLaunchPivot(
        modifier = modifier,
        loadKey = loadKey,
        delayMs = delayMs,
        exiting = exiting,
        skipEnter = skipEnter,
        exitDurationMs = if (exiting) {
            MetroTransitions.ListPivotExitMs
        } else {
            MetroTransitions.TilePivotExitMs
        },
        onExitComplete = onExitComplete,
        onEnterComplete = onEnterComplete,
        content = content,
    )
}

/**
 * Enter/exit delay for [staggerIndex] (0 = chrome). Indices above [maxIndex] share the max
 * delay so long lists stay snappy.
 */
fun metroListPivotEnterDelayMs(
    staggerIndex: Int,
    staggerMs: Int = MetroTransitions.ListPivotStaggerMs,
    maxIndex: Int = MetroTransitions.ListPivotStaggerMaxIndex,
): Long {
    val capped = staggerIndex.coerceIn(0, maxIndex.coerceAtLeast(0))
    return capped.toLong() * staggerMs.coerceAtLeast(0)
}

/**
 * Total time until the last staggered slot finishes its enter swing
 * ([lastStaggerIndex] × stagger + [MetroTransitions.TilePivotEnterOuterMs]).
 */
fun metroListPivotWaveDurationMs(
    lastStaggerIndex: Int,
    staggerMs: Int = MetroTransitions.ListPivotStaggerMs,
    maxIndex: Int = MetroTransitions.ListPivotStaggerMaxIndex,
): Long {
    val delay = metroListPivotEnterDelayMs(lastStaggerIndex, staggerMs, maxIndex)
    return delay + MetroTransitions.TilePivotEnterOuterMs
}

/**
 * Total time until the last staggered slot finishes its exit swing
 * ([lastStaggerIndex] × exit stagger + [MetroTransitions.ListPivotExitMs]).
 */
fun metroListPivotExitWaveDurationMs(
    lastStaggerIndex: Int,
    staggerMs: Int = MetroTransitions.ListPivotExitStaggerMs,
    maxIndex: Int = MetroTransitions.ListPivotExitStaggerMaxIndex,
    exitMs: Int = MetroTransitions.ListPivotExitMs,
): Long {
    val delay = metroListPivotEnterDelayMs(lastStaggerIndex, staggerMs, maxIndex)
    return delay + exitMs
}

/**
 * Coordinates staggered list continuum enter + top-down exit for a page.
 *
 * - [loadKey] / [enterFinished] / [skipEnterFor] / [enterDelayMsFor] — pass into
 *   [MetroStaggeredPivotEnter]
 * - [exiting] — true while the exit cascade runs
 * - [requestExit] — play exit then invoke [onComplete] (e.g. navigate)
 */
@Stable
class MetroListPivotController internal constructor(
    initialLoadKey: Any?,
) {
    var loadKey: Any? by mutableStateOf(initialLoadKey)
        internal set
    var enterFinished: Boolean by mutableStateOf(false)
        internal set
    var exiting: Boolean by mutableStateOf(false)
        private set

    private var waveStartElapsedRealtime: Long = SystemClock.elapsedRealtime()
    private var pendingComplete: (() -> Unit)? = null

    /** Starts the top-down exit cascade; [onComplete] runs after the full exit wave. */
    fun requestExit(onComplete: () -> Unit) {
        if (exiting) return
        pendingComplete = onComplete
        exiting = true
    }

    /**
     * True when this slot should appear at rest (wave finished, or scroll-in after the
     * slot's enter delay has already elapsed — avoids invisible wait then blink).
     */
    fun skipEnterFor(
        staggerIndex: Int,
        staggerMs: Int = MetroTransitions.ListPivotStaggerMs,
    ): Boolean {
        if (exiting) return false
        if (enterFinished) return true
        val full = metroListPivotEnterDelayMs(staggerIndex, staggerMs)
        // Index 0 starts immediately — never skip on elapsed alone during the open wave.
        if (full == 0L) return false
        val elapsed = SystemClock.elapsedRealtime() - waveStartElapsedRealtime
        return elapsed >= full
    }

    /** Remaining enter delay for [staggerIndex], accounting for elapsed wave time. */
    fun enterDelayMsFor(
        staggerIndex: Int,
        staggerMs: Int = MetroTransitions.ListPivotStaggerMs,
    ): Long {
        if (enterFinished || exiting) return 0L
        val full = metroListPivotEnterDelayMs(staggerIndex, staggerMs)
        val elapsed = SystemClock.elapsedRealtime() - waveStartElapsedRealtime
        return (full - elapsed).coerceAtLeast(0L)
    }

    internal fun markEnterFinished() {
        enterFinished = true
    }

    internal fun finishExit() {
        val action = pendingComplete
        pendingComplete = null
        // Clear exiting before remounting so new layers are not treated as mid-exit.
        enterFinished = true
        exiting = false
        loadKey = Any()
        waveStartElapsedRealtime = SystemClock.elapsedRealtime()
        action?.invoke()
    }
}

/**
 * Remembers a [MetroListPivotController] for staggered list enter/exit.
 *
 * [lastStaggerIndex] sizes enter-finished and exit waits (chrome at 0 + list rows). Do not
 * pad up to the stagger cap on exit — that made navigation feel heavily delayed.
 */
@Composable
fun rememberMetroListPivotController(
    lastStaggerIndex: Int,
    loadKey: Any? = Unit,
    staggerMs: Int = MetroTransitions.ListPivotStaggerMs,
): MetroListPivotController {
    val controller = remember(loadKey) { MetroListPivotController(initialLoadKey = loadKey) }
    LaunchedEffect(controller.loadKey, lastStaggerIndex, staggerMs) {
        // After exit restore, [enterFinished] is already true — do not replay enter.
        if (controller.enterFinished) return@LaunchedEffect
        delay(
            metroListPivotWaveDurationMs(
                lastStaggerIndex = lastStaggerIndex,
                staggerMs = staggerMs,
            ),
        )
        controller.markEnterFinished()
    }
    LaunchedEffect(controller.exiting, lastStaggerIndex) {
        if (!controller.exiting) return@LaunchedEffect
        delay(
            metroListPivotExitWaveDurationMs(
                lastStaggerIndex = lastStaggerIndex.coerceAtMost(
                    MetroTransitions.ListPivotExitStaggerMaxIndex,
                ),
            ),
        )
        controller.finishExit()
    }
    return controller
}

/**
 * Remembers whether the staggered enter wave for [loadKey] has finished so LazyColumn
 * rows composed while scrolling can [MetroStaggeredPivotEnter] with `skipEnter = true`.
 *
 * Prefer [rememberMetroListPivotController] when the page also needs exit-on-navigate.
 */
@Composable
fun rememberMetroListPivotEnterWave(
    loadKey: Any? = Unit,
    lastStaggerIndex: Int,
    staggerMs: Int = MetroTransitions.ListPivotStaggerMs,
): MetroListPivotEnterWave {
    var finished by remember(loadKey) { mutableStateOf(false) }
    LaunchedEffect(loadKey, lastStaggerIndex, staggerMs) {
        finished = false
        delay(
            metroListPivotWaveDurationMs(
                lastStaggerIndex = lastStaggerIndex,
                staggerMs = staggerMs,
            ),
        )
        finished = true
    }
    return remember(loadKey, finished) {
        MetroListPivotEnterWave(loadKey = loadKey, finished = finished)
    }
}

/** Snapshot of a list-page staggered enter wave — pair with [MetroStaggeredPivotEnter]. */
data class MetroListPivotEnterWave(
    val loadKey: Any?,
    val finished: Boolean,
)

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 640)
@Composable
private fun MetroStaggeredPivotEnterPreview() {
    MetroTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MetroTheme.colors.background),
        ) {
            Column(Modifier.fillMaxWidth()) {
                MetroStaggeredPivotEnter(staggerIndex = 0) {
                    Column(Modifier.fillMaxWidth()) {
                        MetroAppTitle(title = "SETTINGS")
                        MetroText(
                            text = "system",
                            style = MetroTextStyle.PageTitle,
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp),
                        )
                    }
                }
                listOf("start+theme", "brightness", "storage sense").forEachIndexed { index, title ->
                    MetroStaggeredPivotEnter(staggerIndex = index + 1) {
                        MetroListItem(title = title, onClick = {})
                    }
                }
            }
        }
    }
}
