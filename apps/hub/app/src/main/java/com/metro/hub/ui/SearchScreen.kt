package com.metro.hub.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.metro.hub.R
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/**
 * Music explore–style search: focused [MetroTextBox] + filtered suite catalog rows.
 */
@Composable
fun SearchScreen(
    state: HubState,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observe = generation

    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        searchFocus.requestFocus()
        keyboard?.show()
    }

    Column(modifier = modifier.fillMaxSize()) {
        MetroAppTitle(title = stringResource(R.string.app_name))
        MetroText(
            text = stringResource(R.string.search_title),
            style = MetroTextStyle.HubTitle,
            modifier = Modifier.padding(start = 12.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        MetroTextBox(
            value = state.searchQuery,
            onValueChange = state::updateSearchQuery,
            placeholder = stringResource(R.string.search_placeholder),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .focusRequester(searchFocus),
        )
        Spacer(modifier = Modifier.height(8.dp))

        when {
            state.catalogLoadMode == CatalogLoadMode.Loading && state.allAssets.isEmpty() -> {
                MetroLoadingScreen(
                    message = stringResource(R.string.apps_loading),
                    modifier = Modifier.weight(1f),
                )
            }
            state.releaseError != null && state.allAssets.isEmpty() -> {
                MetroText(
                    text = state.releaseError ?: stringResource(R.string.apps_error),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(12.dp),
                )
            }
            state.searchQuery.isBlank() -> {
                MetroText(
                    text = stringResource(R.string.search_hint),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            state.searchResults.isEmpty() -> {
                MetroText(
                    text = stringResource(R.string.search_empty),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(12.dp),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = ListBottomExtraPadding,
                    ),
                ) {
                    items(
                        state.searchResults,
                        key = { it.firestoreId ?: it.packageName + "/" + it.name },
                    ) { asset ->
                        StoreAppRow(
                            asset = asset,
                            iconPath = state.iconPathFor(asset),
                            onVisible = { state.ensureIcon(asset) },
                            onClick = { state.openAppDetail(asset) },
                        )
                    }
                }
            }
        }
    }
}
