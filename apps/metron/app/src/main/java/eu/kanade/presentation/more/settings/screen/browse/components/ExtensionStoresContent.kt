package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroListItem
import com.metro.ui.MetroSystemIconType
import mihon.domain.extension.model.ExtensionStore
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ExtensionStoresContent(
    repos: List<ExtensionStore>,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onCopy: (ExtensionStore) -> Unit,
    onOpenWebsite: (ExtensionStore) -> Unit,
    onOpenDiscord: (ExtensionStore) -> Unit,
    onClickDelete: (ExtensionStore) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = lazyListState,
        contentPadding = paddingValues,
        modifier = modifier,
    ) {
        items(
            items = repos,
            key = { it.indexUrl },
        ) { store ->
            ExtensionStoresListItem(
                modifier = Modifier.animateItem(),
                store = store,
                onOpenWebsite = { onOpenWebsite(store) },
                onOpenDiscord = { onOpenDiscord(store) },
                onCopy = { onCopy(store) },
                onDelete = { onClickDelete(store) },
            )
        }
    }
}

@Composable
private fun ExtensionStoresListItem(
    store: ExtensionStore,
    onOpenWebsite: () -> Unit,
    onOpenDiscord: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitle = store.contact.website.ifBlank { store.indexUrl }

    MetroListItem(
        title = store.name.lowercase(),
        subtitle = subtitle.lowercase(),
        modifier = modifier,
        onClick = onOpenWebsite,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (store.contact.discord != null) {
                    MetroCircleIconButton(
                        type = MetroSystemIconType.Message,
                        contentDescription = "discord",
                        onClick = onOpenDiscord,
                        glyphSize = 28.dp,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }
                MetroCircleIconButton(
                    type = MetroSystemIconType.Copy,
                    contentDescription = stringResource(MR.strings.action_copy_to_clipboard),
                    onClick = onCopy,
                    glyphSize = 28.dp,
                    modifier = Modifier.padding(start = 2.dp),
                )
                MetroCircleIconButton(
                    type = MetroSystemIconType.Delete,
                    contentDescription = stringResource(MR.strings.action_delete),
                    onClick = onDelete,
                    glyphSize = 28.dp,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
        },
    )
}
