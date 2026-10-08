package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** First onboarding page — welcome copy only (no theme picker; system theme is suite-owned). */
internal class WelcomeStep : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun title(): String = stringResource(MR.strings.onboarding_heading).lowercase()

    @Composable
    override fun Content() {
        MetroText(
            text = stringResource(MR.strings.onboarding_description),
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}
