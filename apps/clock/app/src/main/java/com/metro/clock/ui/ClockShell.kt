package com.metro.clock.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.metro.clock.R
import com.metro.clock.data.AlarmItem
import com.metro.clock.data.AlarmLogic
import com.metro.clock.data.AlarmNotifier
import com.metro.clock.data.ClockAlert
import com.metro.clock.data.ClockAlertBus
import com.metro.clock.data.WorldClockEntry
import com.metro.ui.LocalMetroSubpageExit
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroAppBarTextButton
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroPivot
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import com.metro.ui.metroNavBarPadding
import java.util.Calendar
import kotlinx.coroutines.delay

/** First title is `all` to match WP Alarms list header under the ALARMS overline. */
private val PivotTitles = listOf("all", "world clock", "timer", "stopwatch")

private sealed class ClockRoute {
    data object Pivot : ClockRoute()
    data object AddCity : ClockRoute()
}

/** Slightly smaller trash glyph inside the standard app-bar press circle. */
private val DeleteGlyphSize = 32.dp

/**
 * Clock root. Alarms, world clock, timer, and stopwatch pivots.
 *
 * Pivot pages share one [MetroAppBar] so the chrome stays put while icons bounce in
 * on page change ([MetroAppBar] `enterKey`). New/edit alarm still slides up; city picker
 * uses [MetroSubpageHost].
 */
@Composable
fun ClockShell(
    alarms: List<AlarmItem>,
    worldCities: List<WorldClockEntry>,
    onSaveAlarm: (AlarmItem) -> Unit,
    onDeleteAlarm: (String) -> Unit,
    onToggleAlarm: (String, Boolean) -> Unit,
    onAddWorldCity: (String) -> Unit,
    onDeleteWorldCities: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    pendingAlert: ClockAlert? = null,
    onPendingAlertConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    var editor by remember { mutableStateOf<AlarmEditorTarget?>(null) }
    var renderedEditor by remember { mutableStateOf<AlarmEditorTarget?>(null) }
    if (editor != null) {
        renderedEditor = editor
    }
    var route by remember { mutableStateOf<ClockRoute>(ClockRoute.Pivot) }
    val pagerState = rememberPagerState(pageCount = { PivotTitles.size })

    var alarmsChrome by remember { mutableStateOf<AlarmsChrome?>(null) }
    var worldChrome by remember { mutableStateOf<WorldClockChrome?>(null) }
    var timerChrome by remember { mutableStateOf<TimerChrome?>(null) }
    var stopwatchChrome by remember { mutableStateOf<StopwatchChrome?>(null) }
    var activeAlert by remember { mutableStateOf<ClockAlert?>(null) }
    var alertTone by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopAlertTone() {
        alertTone?.runCatching {
            stop()
            release()
        }
        alertTone = null
    }

    fun dismissAlert() {
        val current = activeAlert
        activeAlert = null
        stopAlertTone()
        if (current is ClockAlert.Alarm) {
            AlarmNotifier.cancelAlarm(context, current.item.id)
        }
        onPendingAlertConsumed()
    }

    fun presentAlert(alert: ClockAlert) {
        if (activeAlert != null) return
        activeAlert = alert
        stopAlertTone()
        alertTone = runCatching {
            MediaPlayer.create(context, R.raw.alarm_generic)?.apply {
                isLooping = true
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                start()
            }
        }.getOrNull()
    }

    DisposableEffect(Unit) {
        ClockAlertBus.setListener { presentAlert(it) }
        onDispose {
            ClockAlertBus.setListener(null)
            stopAlertTone()
        }
    }

    LaunchedEffect(pendingAlert) {
        val alert = pendingAlert ?: return@LaunchedEffect
        presentAlert(alert)
    }

    LaunchedEffect(activeAlert) {
        if (activeAlert == null) return@LaunchedEffect
        delay(5_000L)
        if (activeAlert != null) dismissAlert()
    }

    val pivotPage = pagerState.currentPage
    val showPivotAppBar = route is ClockRoute.Pivot && editor == null

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .metroNavBarPadding()
            .background(MetroTheme.colors.background),
    ) {
        MetroSubpageHost(
            route = route,
            isRoot = { it is ClockRoute.Pivot },
            parentOf = { ClockRoute.Pivot },
            onGoBack = { route = ClockRoute.Pivot },
            modifier = Modifier.fillMaxSize(),
            rootContent = {
                MetroPivot(
                    titles = PivotTitles,
                    pagerState = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    header = { MetroAppTitle(title = "ALARMS") },
                ) { page ->
                    when (page) {
                        0 -> AlarmsListScreen(
                            alarms = alarms,
                            onAdd = {
                                val now = Calendar.getInstance()
                                editor = AlarmEditorTarget.Create(
                                    draft = AlarmItem(
                                        hour = now.get(Calendar.HOUR_OF_DAY),
                                        minute = now.get(Calendar.MINUTE),
                                    ),
                                )
                            },
                            onOpen = { alarm -> editor = AlarmEditorTarget.Edit(alarm) },
                            onToggle = onToggleAlarm,
                            onDelete = onDeleteAlarm,
                            onChromeChange = { alarmsChrome = it },
                            modifier = Modifier.fillMaxSize(),
                        )
                        1 -> WorldClockScreen(
                            entries = worldCities,
                            onAdd = { route = ClockRoute.AddCity },
                            onDelete = onDeleteWorldCities,
                            onChromeChange = { worldChrome = it },
                            modifier = Modifier.fillMaxSize(),
                        )
                        2 -> TimerScreen(
                            onChromeChange = { timerChrome = it },
                            onFinished = { presentAlert(ClockAlert.Timer) },
                            modifier = Modifier.fillMaxSize(),
                        )
                        else -> StopwatchScreen(
                            onChromeChange = { stopwatchChrome = it },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            },
            subpageContent = { current ->
                when (current) {
                    ClockRoute.AddCity -> {
                        val requestExit = LocalMetroSubpageExit.current
                        WorldCityPickerScreen(
                            alreadyAddedIds = worldCities.map { it.city.id }.toSet(),
                            onPick = { city ->
                                onAddWorldCity(city.id)
                                requestExit?.invoke()
                            },
                            onCancel = { requestExit?.invoke() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    ClockRoute.Pivot -> Unit
                }
            },
        )

        if (showPivotAppBar) {
            PivotAppBar(
                page = pivotPage,
                alarmsChrome = alarmsChrome,
                worldChrome = worldChrome,
                timerChrome = timerChrome,
                stopwatchChrome = stopwatchChrome,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        activeAlert?.let { alert ->
            val (title, body) = when (alert) {
                is ClockAlert.Alarm ->
                    AlarmLogic.formatTime(alert.item.hour, alert.item.minute).lowercase() to
                        alert.item.name.ifBlank { "Alarm" }
                ClockAlert.Timer -> "timer" to "time's up"
            }
            MetroMessageDialog(
                title = title,
                body = body,
                confirmLabel = "close",
                onConfirm = { dismissAlert() },
                onDismissRequest = { dismissAlert() },
            )
        }

        AnimatedVisibility(
            visible = editor != null,
            enter = slideInVertically(
                animationSpec = tween(
                    durationMillis = MetroTransitions.PageTransitionMs,
                    easing = MetroTransitions.PageEasing,
                ),
                initialOffsetY = { it },
            ) + fadeIn(
                animationSpec = tween(MetroTransitions.PageTransitionMs),
            ),
            exit = slideOutVertically(
                animationSpec = tween(
                    durationMillis = MetroTransitions.PageTransitionMs,
                    easing = MetroTransitions.PageEasing,
                ),
                targetOffsetY = { it },
            ) + fadeOut(
                animationSpec = tween(MetroTransitions.PageTransitionMs),
            ),
        ) {
            val target = renderedEditor
            if (target != null) {
                BackHandler { editor = null }
                AlarmEditorScreen(
                    initial = target.draft,
                    isNew = target is AlarmEditorTarget.Create,
                    onSave = { saved ->
                        onSaveAlarm(saved)
                        editor = null
                    },
                    onDelete = if (target is AlarmEditorTarget.Edit) {
                        {
                            onDeleteAlarm(target.draft.id)
                            editor = null
                        }
                    } else {
                        null
                    },
                    onCancel = { editor = null },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun PivotAppBar(
    page: Int,
    alarmsChrome: AlarmsChrome?,
    worldChrome: WorldClockChrome?,
    timerChrome: TimerChrome?,
    stopwatchChrome: StopwatchChrome?,
    modifier: Modifier = Modifier,
) {
    when (page) {
        0 -> MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Add,
                    label = "new",
                    onClick = { alarmsChrome?.onAdd?.invoke() },
                ),
            ),
            enterKey = "alarms",
            modifier = modifier,
        )
        1 -> {
            val selecting = worldChrome?.selecting == true
            MetroAppBar(
                icons = if (selecting) {
                    listOf(
                        MetroAppBarIcon(
                            label = "delete",
                            enabled = worldChrome?.canDelete == true,
                            onClick = { worldChrome?.onDeleteSelected?.invoke() },
                            icon = { color ->
                                MetroSystemIcon(
                                    type = MetroSystemIconType.Delete,
                                    iconSize = MetroAppBarDefaults.GlyphSize,
                                    glyphSize = DeleteGlyphSize,
                                    color = color,
                                    showCircle = false,
                                )
                            },
                        ),
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Close,
                            label = "cancel",
                            onClick = { worldChrome?.onCancelSelect?.invoke() },
                        ),
                    )
                } else {
                    listOf(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Add,
                            label = "new",
                            onClick = { worldChrome?.onAdd?.invoke() },
                        ),
                    )
                },
                menuItems = if (worldChrome?.showSelectMenu == true) {
                    listOf(
                        MetroAppBarMenuItem(
                            text = "select",
                            onClick = { worldChrome.onEnterSelect() },
                        ),
                    )
                } else {
                    emptyList()
                },
                enterKey = "world-${if (selecting) "select" else "list"}",
                modifier = modifier,
            )
        }
        2 -> {
            val running = timerChrome?.running == true
            MetroAppBar(
                textButtons = listOf(
                    MetroAppBarTextButton(
                        text = if (running) "pause" else "start",
                        enabled = running || timerChrome?.canStart == true,
                        onClick = {
                            if (running) timerChrome?.onPause?.invoke()
                            else timerChrome?.onStart?.invoke()
                        },
                    ),
                    MetroAppBarTextButton(
                        text = "reset",
                        enabled = timerChrome?.canReset == true,
                        onClick = { timerChrome?.onReset?.invoke() },
                    ),
                ),
                enterKey = "timer-${if (running) "run" else "idle"}",
                modifier = modifier,
            )
        }
        else -> {
            val running = stopwatchChrome?.running == true
            MetroAppBar(
                icons = buildList {
                    if (running) {
                        add(
                            MetroAppBarIcon(
                                type = MetroSystemIconType.Pause,
                                label = "pause",
                                onClick = { stopwatchChrome?.onPause?.invoke() },
                            ),
                        )
                        add(
                            MetroAppBarIcon(
                                type = MetroSystemIconType.Add,
                                label = "lap",
                                onClick = { stopwatchChrome?.onLap?.invoke() },
                            ),
                        )
                    } else {
                        add(
                            MetroAppBarIcon(
                                type = MetroSystemIconType.Play,
                                label = "start",
                                onClick = { stopwatchChrome?.onStart?.invoke() },
                            ),
                        )
                    }
                    add(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Refresh,
                            label = "reset",
                            enabled = stopwatchChrome?.canReset == true,
                            onClick = { stopwatchChrome?.onReset?.invoke() },
                        ),
                    )
                },
                enterKey = "stopwatch-${if (running) "run" else "idle"}",
                modifier = modifier,
            )
        }
    }
}

private sealed class AlarmEditorTarget {
    abstract val draft: AlarmItem

    data class Create(override val draft: AlarmItem) : AlarmEditorTarget()
    data class Edit(override val draft: AlarmItem) : AlarmEditorTarget()
}
