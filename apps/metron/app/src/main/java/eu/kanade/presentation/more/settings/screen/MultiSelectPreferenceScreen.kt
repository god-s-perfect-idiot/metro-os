package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.metro.ui.MetroMultiSelectItem
import com.metro.ui.MetroMultiSelectList
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Full-page multi-select (Apps Corner / connected-apps style) for settings checklists.
 */
class MultiSelectPreferenceScreen(
    private val title: String,
    private val entries: List<Pair<String, String>>,
    private val initialSelected: Set<String>,
    private val onConfirm: (Set<String>) -> Unit,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var draft by remember { mutableStateOf(initialSelected) }
        val items = remember(entries) {
            entries.map { (id, label) ->
                MetroMultiSelectItem(id = id, title = label.lowercase())
            }
        }

        MetroSystemTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .metroNavBarPadding()
                    .background(MetroTheme.colors.background),
            ) {
                MetroMultiSelectList(
                    title = title.lowercase(),
                    items = items,
                    selectedIds = draft,
                    onSelectionChange = { draft = it },
                    onConfirm = {
                        onConfirm(draft)
                        navigator.pop()
                    },
                    onCancel = { navigator.pop() },
                    confirmLabel = stringResource(MR.strings.action_ok).lowercase(),
                    cancelLabel = stringResource(MR.strings.action_cancel).lowercase(),
                )
            }
        }
    }
}
