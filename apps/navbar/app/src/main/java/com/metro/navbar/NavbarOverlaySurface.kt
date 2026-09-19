package com.metro.navbar

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import kotlin.math.roundToInt

/**
 * Hosts the Metro soft-key bar with WP-style creep for immersive hide and display rotation.
 *
 * On rotation the strip slides off the bottom, [onRotateRelayout] refreshes metrics, then the
 * strip slides back in from the new bottom edge.
 */
@Composable
fun NavbarOverlaySurface(
    state: NavbarState,
    displayRotation: Int,
    onRotateRelayout: (() -> Unit)? = null,
    onBack: () -> Unit,
    onBackLongPress: () -> Unit,
    onStart: () -> Unit,
    onSearch: () -> Unit,
    onSearchLongPress: () -> Unit,
) {
    val chromeOnScreen = state.showFullBar || state.showRevealStrip
    var painted by remember { mutableStateOf(chromeOnScreen) }
    val barOffset = remember { Animatable(if (chromeOnScreen) 0f else 1f) }
    var previousRotation by remember { mutableStateOf(displayRotation) }
    val density = LocalDensity.current
    val slidePx = with(density) { NavbarSpec.BAR_HEIGHT_DP.dp.toPx() }

    LaunchedEffect(chromeOnScreen) {
        if (chromeOnScreen) {
            barOffset.snapTo(1f)
            painted = true
            barOffset.animateTo(0f, MetroTransitions.appBarCreepTween())
        } else if (painted) {
            barOffset.animateTo(1f, MetroTransitions.appBarCreepTween())
            painted = false
        }
    }

    LaunchedEffect(displayRotation) {
        val fromRotation = previousRotation
        if (fromRotation == displayRotation) return@LaunchedEffect
        previousRotation = displayRotation
        if (!chromeOnScreen || !painted) {
            onRotateRelayout?.invoke()
            return@LaunchedEffect
        }
        barOffset.animateTo(1f, MetroTransitions.appBarCreepTween())
        onRotateRelayout?.invoke()
        barOffset.animateTo(0f, MetroTransitions.appBarCreepTween())
    }

    if (!painted) return

    MetroTheme(
        darkTheme = state.theme.darkTheme,
        accent = state.theme.accentColor,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .offset { IntOffset(0, (barOffset.value * slidePx).roundToInt()) }
                .then(
                    if (state.showFullBar) {
                        Modifier.pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -24f) {
                                    state.hide()
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (state.showFullBar) {
                NavbarWithSystemChrome(
                    theme = state.theme,
                    onBack = onBack,
                    onBackLongPress = onBackLongPress,
                    onStart = onStart,
                    onSearch = onSearch,
                    onSearchLongPress = onSearchLongPress,
                )
            } else if (state.showRevealStrip) {
                HiddenNavbarWithSystemChrome(
                    theme = state.theme,
                    onReveal = { state.show() },
                )
            }
        }
    }
}
