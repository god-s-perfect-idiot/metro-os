package eu.kanade.presentation.more.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import dev.icerock.moko.resources.StringResource
import tachiyomi.presentation.core.i18n.stringResource

/** Extra space so the last preference rows clear the bottom app bar + soft keys. */
private val ContentBottomClearance = MetroAppBarDefaults.BarHeight + 32.dp

@Composable
fun PreferenceScaffold(
    titleRes: StringResource,
    onBackPressed: (() -> Unit)? = null,
    onHelpClick: (() -> Unit)? = null,
    itemsProvider: @Composable () -> List<Preference>,
) {
    // System Back handles navigation — never put Back in the Metro app bar.
    @Suppress("UNUSED_PARAMETER")
    val unusedBack = onBackPressed

    MetroSystemTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .metroNavBarPadding()
                .background(MetroTheme.colors.background),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MetroSettingsHeader(
                    pageTitle = stringResource(titleRes).lowercase(),
                    appTitle = "metron",
                )
                PreferenceScreen(
                    items = itemsProvider(),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = ContentBottomClearance),
                )
            }

            if (onHelpClick != null) {
                MetroAppBar(
                    menuItems = listOf(
                        MetroAppBarMenuItem("help", onClick = onHelpClick),
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
