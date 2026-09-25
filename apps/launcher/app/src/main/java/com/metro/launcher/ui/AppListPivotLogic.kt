package com.metro.launcher.ui

import com.metro.system.MetroAppInfo
import com.metro.ui.MetroTransitions

/**
 * App-list exit peel stagger — bottom→top with the tapped row last.
 * Wave includes search chrome, letter markers, and app rows.
 * Enter is static (no wave).
 */
internal object AppListPivotLogic {
    /** Match Start tile diagonal step so the weave reads the same speed. */
    const val StaggerMs = TileEnterStaggerMs.toInt()

    /**
     * Cap so long alphabetical lists stay snappy.
     */
    const val StaggerMaxStep = 12

    const val ExitDefaultMs = MetroTransitions.TilePivotExitMs
    const val ExitSelectedMs = MetroTransitions.TilePivotExitSelectedMs

    /** Stable wave key for the search circle. */
    const val SearchWaveKey = "__search__"

    fun letterWaveKey(letter: Char): String = "header-$letter"

    /**
     * Top→bottom wave keys: search, then each letter marker + its apps.
     * Used to identify rows; exit stagger is bottom→top over the visible subset.
     */
    fun buildWaveKeys(
        appsByLetter: Map<Char, List<MetroAppInfo>>,
        includeLetters: Boolean,
    ): List<String> = buildList {
        add(SearchWaveKey)
        appsByLetter.forEach { (letter, sectionApps) ->
            if (includeLetters) add(letterWaveKey(letter))
            sectionApps.forEach { add(it.packageName) }
        }
    }

    /**
     * Exit stagger step for [rowIndex] (0 = top). Bottom rows exit first; the tapped
     * row is scheduled after the last bottom→top beat.
     */
    fun exitStepIndex(
        rowIndex: Int,
        lastIndex: Int,
        selected: Boolean,
    ): Int {
        if (lastIndex < 0) return 0
        if (selected) return lastIndex + 1
        return (lastIndex - rowIndex).coerceAtLeast(0)
    }

    fun exitDelayMs(
        rowIndex: Int,
        lastIndex: Int,
        selected: Boolean,
        staggerMs: Int = StaggerMs,
        maxStep: Int = StaggerMaxStep,
    ): Long {
        val cappedMax = maxStep.coerceAtLeast(0)
        val step = if (selected) {
            lastIndex.coerceIn(0, cappedMax) + 1
        } else {
            exitStepIndex(rowIndex, lastIndex, selected = false)
                .coerceAtMost(cappedMax)
        }
        return step.toLong() * staggerMs.coerceAtLeast(0)
    }

    fun exitWaveDurationMs(
        lastIndex: Int,
        staggerMs: Int = StaggerMs,
        maxStep: Int = StaggerMaxStep,
    ): Long {
        if (lastIndex < 0) return ExitSelectedMs.toLong()
        return exitDelayMs(
            rowIndex = 0,
            lastIndex = lastIndex,
            selected = true,
            staggerMs = staggerMs,
            maxStep = maxStep,
        ) + ExitSelectedMs
    }
}
