package com.metro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Request the current subpage’s [MetroPagePivotLoad] flip-out. Provided inside
 * [MetroSubpageHost]’s [subpageContent]. Wire child `onBack` / [BackHandler]s to this so Back
 * animates out instead of popping immediately.
 */
val LocalMetroSubpageExit = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * Suite-standard in-app page stack.
 *
 * - **Root** ([isRoot]) renders [rootContent] with **no** page-pivot (panorama intro, pivot
 *   chrome, Start, and other roots keep their own motion).
 * - **Every non-root route** enters and exits with [MetroPagePivotLoad] (200ms / 280ms).
 * - Back starts the exit swing; [onGoBack] runs only after the outro completes.
 * - Returning to a parent after exit skips that parent’s enter swing ([skipEnter]).
 *
 * **Do not use** for: Start / launcher tile motion, panorama hub surfaces, shell overlays
 * (lock screen, volume HUD, status tray, toast, soft keys), or in-call / incoming-call UIs.
 * Setup / configuration activities for those shell apps **do** use this for their drill-ins.
 */
@Composable
fun <R : Any> MetroSubpageHost(
    route: R,
    isRoot: (R) -> Boolean,
    parentOf: (R) -> R,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier,
    loadKeyOf: (R) -> Any = { it },
    rootContent: @Composable () -> Unit,
    subpageContent: @Composable (R) -> Unit,
) {
    var exitingRoute by remember { mutableStateOf<R?>(null) }
    var suppressEnterFor by remember { mutableStateOf<R?>(null) }
    val isExiting = exitingRoute != null
    val onRoot = isRoot(route) && !isExiting

    LaunchedEffect(route, suppressEnterFor) {
        if (suppressEnterFor != null && route != suppressEnterFor) {
            suppressEnterFor = null
        }
    }

    val requestExit: () -> Unit = {
        if (!isExiting && !isRoot(route)) {
            exitingRoute = route
        }
    }

    BackHandler(enabled = !onRoot && !isExiting) {
        requestExit()
    }

    BackHandler(enabled = isExiting) {
        // Hold the stack until the flip-out finishes.
    }

    Box(modifier = modifier) {
        when {
            isExiting -> {
                val leaving = exitingRoute!!
                MetroSubpagePivot(
                    loadKey = loadKeyOf(leaving),
                    exiting = true,
                    onExitComplete = {
                        suppressEnterFor = parentOf(leaving)
                        onGoBack()
                        exitingRoute = null
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CompositionLocalProvider(LocalMetroSubpageExit provides requestExit) {
                        subpageContent(leaving)
                    }
                }
            }
            onRoot -> rootContent()
            else -> {
                MetroSubpagePivot(
                    loadKey = loadKeyOf(route),
                    skipEnter = route == suppressEnterFor,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CompositionLocalProvider(LocalMetroSubpageExit provides requestExit) {
                        subpageContent(route)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetroSubpagePivot(
    loadKey: Any,
    modifier: Modifier = Modifier,
    exiting: Boolean = false,
    skipEnter: Boolean = false,
    onExitComplete: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    MetroPagePivotLoad(
        modifier = modifier.background(MetroTheme.colors.background),
        loadKey = loadKey,
        exiting = exiting,
        skipEnter = skipEnter,
        onExitComplete = onExitComplete,
        content = content,
    )
}
