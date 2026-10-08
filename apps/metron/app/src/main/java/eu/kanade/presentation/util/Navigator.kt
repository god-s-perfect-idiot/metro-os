package eu.kanade.presentation.util

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScreenTransitionContent
import com.metro.ui.LocalMetroSubpageExit
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemTheme

/**
 * For invoking back press to the parent activity
 */
val LocalBackPress: ProvidableCompositionLocal<(() -> Unit)?> = staticCompositionLocalOf { null }

interface Tab : cafe.adriel.voyager.navigator.tab.Tab {
    suspend fun onReselect(navigator: Navigator) {}
}

abstract class Screen : Screen {

    override val key: ScreenKey = uniqueScreenKey
}

interface AssistContentScreen {
    fun onProvideAssistUrl(): String?
}

/**
 * Suite page-pivot enter/exit for every Voyager screen that is not the navigator root
 * (panorama [eu.kanade.tachiyomi.ui.home.HomeScreen] stays unpivoted).
 */
@Composable
fun DefaultNavigatorScreenTransition(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val current = navigator.lastItem
    val root = navigator.items.first()
    MetroSystemTheme {
        MetroSubpageHost(
            route = current,
            isRoot = { it.key == root.key },
            parentOf = { screen ->
                val items = navigator.items
                val index = items.indexOfLast { it.key == screen.key }
                if (index > 0) items[index - 1] else root
            },
            loadKeyOf = { it.key },
            onGoBack = { navigator.pop() },
            modifier = modifier,
            rootContent = {
                navigator.saveableState("metro_subpage", root) {
                    root.Content()
                }
            },
            subpageContent = { screen ->
                val requestExit = LocalMetroSubpageExit.current
                navigator.saveableState("metro_subpage", screen) {
                    CompositionLocalProvider(
                        LocalBackPress provides {
                            requestExit?.invoke() ?: navigator.pop()
                        },
                    ) {
                        screen.Content()
                    }
                }
            },
        )
    }
}

@Composable
fun ScreenTransition(
    navigator: Navigator,
    transition: AnimatedContentTransitionScope<Screen>.() -> ContentTransform,
    modifier: Modifier = Modifier,
    content: ScreenTransitionContent = { it.Content() },
) {
    AnimatedContent(
        targetState = navigator.lastItem,
        transitionSpec = transition,
        modifier = modifier,
        label = "transition",
    ) { screen ->
        navigator.saveableState("transition", screen) {
            content(screen)
        }
    }

    BackHandler(enabled = navigator.canPop, onBack = navigator::pop)
}
