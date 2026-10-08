package com.metro.metron.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroTransitions

private val OptionsCardMaxHeight = 360.dp

/**
 * Options panel that slides up from the bottom edge over [com.metro.ui.MetroAppBar],
 * matching app-bar chrome. Slides back down and leaves composition when dismissed.
 */
@Composable
fun MetronOptionsCard(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val transition = remember { MutableTransitionState(false) }
    transition.targetState = visible
    val showing = transition.currentState || transition.targetState
    if (!showing) return

    val slideSpec = MetroTransitions.appBarCreepTween<IntOffset>()
    val fadeSpec = MetroTransitions.appBarCreepTween<Float>()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visibleState = transition,
            enter = fadeIn(animationSpec = fadeSpec),
            exit = fadeOut(animationSpec = fadeSpec),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visibleState = transition,
            enter = slideInVertically(
                animationSpec = slideSpec,
                initialOffsetY = { fullHeight -> fullHeight },
            ) + fadeIn(animationSpec = fadeSpec),
            exit = slideOutVertically(
                animationSpec = slideSpec,
                targetOffsetY = { fullHeight -> fullHeight },
            ) + fadeOut(animationSpec = fadeSpec),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = OptionsCardMaxHeight)
                    .background(MetroAppBarDefaults.ChromeBackground)
                    .navigationBarsPadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .verticalScroll(rememberScrollState()),
                content = content,
            )
        }
    }
}
