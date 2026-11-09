package com.metro.clock.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.clock.data.TimerLogic
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import kotlinx.coroutines.delay

data class StopwatchLap(
    val index: Int,
    val splitMs: Long,
    val totalMs: Long,
)

data class StopwatchChrome(
    val running: Boolean,
    val canReset: Boolean,
    val onStart: () -> Unit,
    val onPause: () -> Unit,
    val onLap: () -> Unit,
    val onReset: () -> Unit,
)

@Composable
fun StopwatchScreen(
    onChromeChange: (StopwatchChrome) -> Unit,
    modifier: Modifier = Modifier,
) {
    var running by remember { mutableStateOf(false) }
    var baseElapsedMs by remember { mutableLongStateOf(0L) }
    var startedAtMs by remember { mutableLongStateOf(0L) }
    var displayMs by remember { mutableLongStateOf(0L) }
    var laps by remember { mutableStateOf<List<StopwatchLap>>(emptyList()) }
    var lastLapAtMs by remember { mutableLongStateOf(0L) }

    fun currentElapsed(): Long =
        if (running) baseElapsedMs + (System.currentTimeMillis() - startedAtMs) else baseElapsedMs

    LaunchedEffect(running, startedAtMs, baseElapsedMs) {
        if (!running) {
            displayMs = baseElapsedMs
            return@LaunchedEffect
        }
        while (running) {
            displayMs = baseElapsedMs + (System.currentTimeMillis() - startedAtMs)
            delay(32L)
        }
    }

    LaunchedEffect(running, baseElapsedMs, laps.size) {
        onChromeChange(
            StopwatchChrome(
                running = running,
                canReset = !running && (baseElapsedMs > 0L || laps.isNotEmpty()),
                onStart = {
                    startedAtMs = System.currentTimeMillis()
                    running = true
                },
                onPause = {
                    baseElapsedMs = currentElapsed()
                    running = false
                },
                onLap = {
                    val total = currentElapsed()
                    val split = total - lastLapAtMs
                    lastLapAtMs = total
                    val nextIndex = (laps.firstOrNull()?.index ?: 0) + 1
                    laps = listOf(
                        StopwatchLap(
                            index = nextIndex,
                            splitMs = split,
                            totalMs = total,
                        ),
                    ) + laps
                },
                onReset = {
                    baseElapsedMs = 0L
                    displayMs = 0L
                    lastLapAtMs = 0L
                    laps = emptyList()
                },
            ),
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        BasicText(
            text = TimerLogic.formatStopwatch(displayMs),
            style = MetroTextStyle.PageTitle
                .toTextStyle(MetroTheme.fontFamily)
                .copy(
                    color = MetroTheme.colors.primaryText,
                    fontSize = 88.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center,
                ),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-56).dp)
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        if (laps.isNotEmpty()) {
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = MetroAppBarDefaults.BarHeight),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight * 0.38f),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                ) {
                    itemsIndexed(laps, key = { _, lap -> lap.index }) { _, lap ->
                        MetroListItem(
                            title = "Lap ${lap.index}",
                            subtitle = "${TimerLogic.formatStopwatch(lap.splitMs)}  ·  ${TimerLogic.formatStopwatch(lap.totalMs)}",
                            oneLineMinHeight = 56.dp,
                            twoLineMinHeight = 64.dp,
                            verticalPadding = 6.dp,
                        )
                    }
                }
            }
        }
    }
}
