package eu.kanade.presentation.more.onboarding

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import eu.kanade.presentation.more.settings.screen.SettingsDataScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal class StorageStep : OnboardingStep {

    private var _isComplete by mutableStateOf(false)

    override val isComplete: Boolean
        get() = _isComplete

    @Composable
    override fun title(): String = stringResource(MR.strings.pref_storage_location).lowercase()

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val handler = LocalUriHandler.current

        val storagePref = remember { context.appGraph.storagePreferences.baseStorageDirectory }
        val pickStorageLocation = SettingsDataScreen.storageLocationPicker(storagePref)

        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MetroText(
                text = stringResource(
                    MR.strings.onboarding_storage_info,
                    stringResource(MR.strings.app_name),
                    SettingsDataScreen.storageLocationText(storagePref),
                ),
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText,
            )

            MetroBorderButton(
                text = stringResource(MR.strings.onboarding_storage_action_select).lowercase(),
                onClick = {
                    try {
                        pickStorageLocation.launch(null)
                    } catch (_: ActivityNotFoundException) {
                        context.toast(MR.strings.file_picker_error)
                    }
                },
            )

            MetroText(
                text = stringResource(
                    MR.strings.onboarding_storage_help_info,
                    stringResource(MR.strings.app_name),
                ),
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
            )

            MetroBorderButton(
                text = stringResource(MR.strings.onboarding_storage_help_action).lowercase(),
                onClick = { handler.openUri(SettingsDataScreen.HELP_URL) },
            )
        }

        LaunchedEffect(Unit) {
            storagePref.changes()
                .collectLatest { _isComplete = storagePref.isSet() }
        }
    }
}
