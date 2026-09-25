package com.metro.launcher

import com.metro.launcher.ui.AppListPivotLogic
import com.metro.system.MetroAppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class AppListPivotLogicTest {

    @Test
    fun exitStep_bottomRowsFirst_selectedLast() {
        // 4 apps (indices 0..3): bottom (3) step 0, top (0) step 3, selected step 4
        assertEquals(0, AppListPivotLogic.exitStepIndex(3, lastIndex = 3, selected = false))
        assertEquals(1, AppListPivotLogic.exitStepIndex(2, lastIndex = 3, selected = false))
        assertEquals(3, AppListPivotLogic.exitStepIndex(0, lastIndex = 3, selected = false))
        assertEquals(4, AppListPivotLogic.exitStepIndex(1, lastIndex = 3, selected = true))
    }

    @Test
    fun exitDelay_matchesTileStaggerAndCapsLongLists() {
        assertEquals(0L, AppListPivotLogic.exitDelayMs(9, lastIndex = 9, selected = false))
        assertEquals(
            AppListPivotLogic.StaggerMs.toLong(),
            AppListPivotLogic.exitDelayMs(8, lastIndex = 9, selected = false),
        )
        // Far-from-bottom rows share the max step.
        assertEquals(
            AppListPivotLogic.StaggerMaxStep.toLong() * AppListPivotLogic.StaggerMs,
            AppListPivotLogic.exitDelayMs(0, lastIndex = 40, selected = false),
        )
        // Selected is one beat after the capped cascade.
        assertEquals(
            (AppListPivotLogic.StaggerMaxStep + 1L) * AppListPivotLogic.StaggerMs,
            AppListPivotLogic.exitDelayMs(0, lastIndex = 40, selected = true),
        )
    }

    @Test
    fun exitWaveDuration_includesSelectedBeat() {
        // lastIndex 3 → selected delay step 4 → 4×40 + selected ms
        assertEquals(
            4L * AppListPivotLogic.StaggerMs + AppListPivotLogic.ExitSelectedMs,
            AppListPivotLogic.exitWaveDurationMs(lastIndex = 3),
        )
        assertEquals(
            AppListPivotLogic.ExitSelectedMs.toLong(),
            AppListPivotLogic.exitWaveDurationMs(lastIndex = -1),
        )
    }

    @Test
    fun buildWaveKeys_includeSearchThenLettersThenApps() {
        val apps = listOf(
            MetroAppInfo(
                packageName = "a.one",
                label = "Alpha",
                isSystemApp = false,
                isPinned = false,
            ),
            MetroAppInfo(
                packageName = "b.two",
                label = "Beta",
                isSystemApp = false,
                isPinned = false,
            ),
        )
        val grouped = apps.groupBy { it.label.first().lowercaseChar() }
        assertEquals(
            listOf(
                AppListPivotLogic.SearchWaveKey,
                "header-a",
                "a.one",
                "header-b",
                "b.two",
            ),
            AppListPivotLogic.buildWaveKeys(grouped, includeLetters = true),
        )
        assertEquals(
            listOf(
                AppListPivotLogic.SearchWaveKey,
                "a.one",
                "b.two",
            ),
            AppListPivotLogic.buildWaveKeys(grouped, includeLetters = false),
        )
    }
}
