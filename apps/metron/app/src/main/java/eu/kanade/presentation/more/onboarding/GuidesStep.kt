package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal class GuidesStep(
    private val onRestoreBackup: () -> Unit,
) : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun title(): String = stringResource(MR.strings.pref_onboarding_guide).lowercase()

    @Composable
    override fun Content() {
        val handler = LocalUriHandler.current

        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MetroText(
                text = stringResource(
                    MR.strings.onboarding_guides_new_user,
                    stringResource(MR.strings.app_name),
                ),
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText,
            )
            MetroBorderButton(
                text = stringResource(MR.strings.getting_started_guide).lowercase(),
                onClick = { handler.openUri(GETTING_STARTED_URL) },
            )

            MetroText(
                text = stringResource(
                    MR.strings.onboarding_guides_returning_user,
                    stringResource(MR.strings.app_name),
                ),
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText,
            )
            MetroBorderButton(
                text = stringResource(MR.strings.pref_restore_backup).lowercase(),
                onClick = onRestoreBackup,
            )
        }
    }
}

const val GETTING_STARTED_URL = "https://mihon.app/docs/guides/getting-started"
