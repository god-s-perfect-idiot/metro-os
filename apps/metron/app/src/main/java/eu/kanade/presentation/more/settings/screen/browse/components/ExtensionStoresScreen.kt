package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoreScreenState
import mihon.domain.extension.model.ExtensionStore
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ExtensionStoresScreen(
    state: ExtensionStoreScreenState.Success,
    onClickCreate: () -> Unit,
    onCopy: (ExtensionStore) -> Unit,
    onOpenWebsite: (ExtensionStore) -> Unit,
    onOpenDiscord: (ExtensionStore) -> Unit,
    onClickDelete: (ExtensionStore) -> Unit,
    onClickRefresh: () -> Unit,
    navigateUp: () -> Unit,
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedNavigateUp = navigateUp
    val lazyListState = rememberLazyListState()

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
                    pageTitle = stringResource(MR.strings.extensionStores).lowercase(),
                    appTitle = "metron",
                )
                if (state.isEmpty) {
                    MetroEmptyState(
                        message = stringResource(MR.strings.extensionStoresScreen_emptyLabel).lowercase(),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = MetroAppBarDefaults.BarHeight),
                    )
                } else {
                    ExtensionStoresContent(
                        repos = state.stores,
                        lazyListState = lazyListState,
                        paddingValues = PaddingValues(bottom = MetroAppBarDefaults.BarHeight + 32.dp),
                        onCopy = onCopy,
                        onOpenWebsite = onOpenWebsite,
                        onOpenDiscord = onOpenDiscord,
                        onClickDelete = onClickDelete,
                    )
                }
            }

            MetroAppBar(
                icons = listOf(
                    MetroAppBarIcon(
                        type = MetroSystemIconType.Add,
                        label = stringResource(MR.strings.action_add).lowercase(),
                        onClick = onClickCreate,
                    ),
                    MetroAppBarIcon(
                        type = MetroSystemIconType.Refresh,
                        label = stringResource(MR.strings.action_webview_refresh).lowercase(),
                        onClick = onClickRefresh,
                    ),
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
