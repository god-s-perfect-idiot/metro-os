package eu.kanade.presentation.more.onboarding

import androidx.compose.runtime.Composable

internal interface OnboardingStep {

    val isComplete: Boolean

    @Composable
    fun title(): String

    @Composable
    fun Content()
}
