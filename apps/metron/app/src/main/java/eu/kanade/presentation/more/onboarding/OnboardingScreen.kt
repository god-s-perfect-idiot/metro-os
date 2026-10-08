package eu.kanade.presentation.more.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRestoreBackup: () -> Unit,
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val steps = remember {
        listOf(
            WelcomeStep(),
            StorageStep(),
            PermissionStep(),
            GuidesStep(onRestoreBackup = onRestoreBackup),
        )
    }
    val isLastStep = currentStep == steps.lastIndex
    val step = steps[currentStep]
    val actionLabel = stringResource(
        if (isLastStep) {
            MR.strings.onboarding_action_finish
        } else {
            MR.strings.onboarding_action_next
        },
    ).lowercase()

    BackHandler(enabled = currentStep != 0) {
        currentStep--
    }

    MetroSystemTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .metroNavBarPadding()
                .background(MetroTheme.colors.background),
        ) {
            AnimatedContent(
                targetState = currentStep,
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
                transitionSpec = {
                    val forward = targetState > initialState
                    val slide = tween<IntOffset>(
                        durationMillis = MetroTransitions.PageTransitionMs,
                        easing = MetroTransitions.PageEasing,
                    )
                    val enter = slideInHorizontally(
                        animationSpec = slide,
                        initialOffsetX = { fullWidth -> if (forward) fullWidth else -fullWidth },
                    )
                    val exit = slideOutHorizontally(
                        animationSpec = slide,
                        targetOffsetX = { fullWidth -> if (forward) -fullWidth else fullWidth },
                    )
                    enter togetherWith exit
                },
                label = "onboardingStep",
            ) { index ->
                Column(modifier = Modifier.fillMaxSize()) {
                    MetroSettingsHeader(
                        pageTitle = steps[index].title(),
                        appTitle = "metron",
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                PaddingValues(bottom = MetroAppBarDefaults.BarHeight + 32.dp),
                            ),
                    ) {
                        steps[index].Content()
                    }
                }
            }

            // Store-style half-width verb only — no app-bar chrome or ellipsis.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(MetroAppBarDefaults.BarHeight)
                    .padding(start = 8.dp, end = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OnboardingActionButton(
                    text = actionLabel,
                    enabled = step.isComplete,
                    enterKey = currentStep,
                    onClick = {
                        if (isLastStep) {
                            onComplete()
                        } else {
                            currentStep++
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = MetroAppBarDefaults.TextButtonVerticalInset),
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** App-bar text-button look + overshoot enter, without the bar chrome. */
@Composable
private fun OnboardingActionButton(
    text: String,
    enabled: Boolean,
    enterKey: Any,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val foreground = MetroTheme.colors.primaryText
    val borderColor = if (enabled) foreground else foreground.copy(alpha = 0.4f)
    val textColor = if (enabled) foreground else foreground.copy(alpha = 0.4f)
    val background = when {
        !enabled -> Color.Transparent
        pressed -> foreground.copy(alpha = 0.2f)
        else -> Color.Transparent
    }
    val buttonHeightPx = with(LocalDensity.current) { MetroAppBarDefaults.TouchTarget.toPx() }
    val offsetAnim = remember { Animatable(0f) }
    val opacityAnim = remember { Animatable(1f) }

    LaunchedEffect(enterKey) {
        offsetAnim.snapTo(MetroTransitions.AppBarButtonStartOffsetFraction)
        opacityAnim.snapTo(0f)
        launch {
            opacityAnim.animateTo(1f, MetroTransitions.appBarCreepTween())
        }
        offsetAnim.animateTo(0f, MetroTransitions.appBarButtonOvershootKeyframes())
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = offsetAnim.value * buttonHeightPx
                alpha = opacityAnim.value
            }
            .background(background, RectangleShape)
            .border(width = 2.dp, color = borderColor, shape = RectangleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                fontFamily = MetroTheme.fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = textColor,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
