package com.metro.clock.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.metro.clock.data.TimerLogic
import com.metro.clock.data.TimerScheduler
import kotlinx.coroutines.delay

/**
 * Timer pivot — duration is the same accent-tile preview as the time picker
 * (hours | minutes | seconds), hosted in-page with no nested app title.
 */
@Composable
fun TimerScreen(
    onChromeChange: (TimerChrome) -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var setHours by remember { mutableIntStateOf(0) }
    var setMinutes by remember { mutableIntStateOf(1) }
    var setSeconds by remember { mutableIntStateOf(0) }
    var displayHours by remember { mutableIntStateOf(0) }
    var displayMinutes by remember { mutableIntStateOf(1) }
    var displaySeconds by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember {
        mutableIntStateOf(TimerLogic.totalSeconds(0, 1, 0))
    }
    var running by remember { mutableStateOf(false) }
    var endAtMillis by remember { mutableLongStateOf(0L) }
    var completedInForeground by remember { mutableStateOf(false) }

    val setTotal = TimerLogic.totalSeconds(setHours, setMinutes, setSeconds)

    fun applyDisplay(total: Int) {
        val (h, m, s) = TimerLogic.partsFromTotalSeconds(total)
        displayHours = h.coerceIn(0, 23)
        displayMinutes = m
        displaySeconds = s
        remainingSeconds = total
    }

    LaunchedEffect(running, endAtMillis) {
        if (!running) return@LaunchedEffect
        while (running) {
            val left = ((endAtMillis - System.currentTimeMillis() + 999L) / 1000L)
                .toInt()
                .coerceAtLeast(0)
            applyDisplay(left)
            if (left == 0) {
                running = false
                completedInForeground = true
                TimerScheduler.cancel(context)
                break
            }
            delay(200L)
        }
    }

    LaunchedEffect(completedInForeground) {
        if (!completedInForeground) return@LaunchedEffect
        completedInForeground = false
        applyDisplay(setTotal)
        setHours = displayHours
        setMinutes = displayMinutes
        setSeconds = displaySeconds
        onFinished()
    }

    LaunchedEffect(running, remainingSeconds, setTotal) {
        onChromeChange(
            TimerChrome(
                running = running,
                canStart = remainingSeconds > 0,
                canReset = !running && remainingSeconds != setTotal,
                onStart = {
                    endAtMillis = System.currentTimeMillis() + remainingSeconds * 1000L
                    running = true
                    completedInForeground = false
                    TimerScheduler.schedule(context, endAtMillis)
                },
                onPause = {
                    val left = ((endAtMillis - System.currentTimeMillis() + 999L) / 1000L)
                        .toInt()
                        .coerceAtLeast(0)
                    applyDisplay(left)
                    running = false
                    TimerScheduler.cancel(context)
                },
                onReset = {
                    applyDisplay(setTotal)
                    setHours = TimerLogic.partsFromTotalSeconds(setTotal).first.coerceIn(0, 23)
                    setMinutes = TimerLogic.partsFromTotalSeconds(setTotal).second
                    setSeconds = TimerLogic.partsFromTotalSeconds(setTotal).third
                    completedInForeground = false
                    TimerScheduler.cancel(context)
                },
            ),
        )
    }

    DurationTiles(
        hours = displayHours,
        minutes = displayMinutes,
        seconds = displaySeconds,
        onHoursChange = {
            displayHours = it
            setHours = it
            remainingSeconds = TimerLogic.totalSeconds(it, displayMinutes, displaySeconds)
        },
        onMinutesChange = {
            displayMinutes = it
            setMinutes = it
            remainingSeconds = TimerLogic.totalSeconds(displayHours, it, displaySeconds)
        },
        onSecondsChange = {
            displaySeconds = it
            setSeconds = it
            remainingSeconds = TimerLogic.totalSeconds(displayHours, displayMinutes, it)
        },
        interactive = !running,
        modifier = modifier.fillMaxSize(),
    )
}

data class TimerChrome(
    val running: Boolean,
    val canStart: Boolean,
    val canReset: Boolean,
    val onStart: () -> Unit,
    val onPause: () -> Unit,
    val onReset: () -> Unit,
)
