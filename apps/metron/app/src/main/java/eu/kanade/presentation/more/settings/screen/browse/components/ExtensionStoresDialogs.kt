package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ExtensionStoreCreateDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
    storeIndexUrls: Set<String>,
    processing: Boolean,
    errorMessage: String?,
) {
    var url by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val storeAlreadyExists = remember(url, storeIndexUrls) { storeIndexUrls.contains(url) }
    val canCreate = url.isNotEmpty() && !storeAlreadyExists && !processing

    MetroMessageDialog(
        title = stringResource(MR.strings.extensionStoresScreen_addStore_title).lowercase(),
        onDismissRequest = onDismissRequest,
        confirmLabel = stringResource(
            if (processing) {
                MR.strings.extensionStoresScreen_addStore_processing
            } else {
                MR.strings.action_add
            },
        ).lowercase(),
        onConfirm = {
            if (canCreate) {
                onCreate(url)
            }
        },
        dismissLabel = stringResource(MR.strings.action_cancel).lowercase(),
        content = {
            MetroTextBox(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = stringResource(
                    MR.strings.extensionStoresScreen_addStoreInput_inputLabel,
                ).lowercase(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (canCreate) {
                            onCreate(url)
                        }
                    },
                ),
            )
            val support = when {
                errorMessage != null -> errorMessage
                storeAlreadyExists -> stringResource(MR.strings.extensionStoresScreen_addStore_alreadyExists)
                else -> stringResource(MR.strings.information_required_plain)
            }
            MetroText(
                text = support.lowercase(),
                style = MetroTextStyle.ListItemSubtitle,
                color = if (errorMessage != null || storeAlreadyExists) {
                    MetroTheme.colors.accent
                } else {
                    MetroTheme.colors.secondaryText
                },
                modifier = Modifier.padding(top = 8.dp),
            )
        },
    )

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}

@Composable
fun ExtensionStoreDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    storeName: String,
    storeIndexUrl: String,
) {
    MetroMessageDialog(
        title = stringResource(MR.strings.extensionStoresScreen_deleteStore_title).lowercase(),
        body = stringResource(
            MR.strings.extensionStoresScreen_deleteStore_body,
            storeName,
            storeIndexUrl,
        ).lowercase(),
        onDismissRequest = onDismissRequest,
        confirmLabel = stringResource(MR.strings.action_ok).lowercase(),
        onConfirm = {
            onDelete()
            onDismissRequest()
        },
        dismissLabel = stringResource(MR.strings.action_cancel).lowercase(),
    )
}

@Composable
fun ExtensionStoreConfirmDialog(
    onDismissRequest: () -> Unit,
    onCreate: () -> Unit,
    storeIndexUrl: String,
    storeAlreadyExists: Boolean,
    processing: Boolean,
    errorMessage: String?,
) {
    MetroMessageDialog(
        title = stringResource(MR.strings.extensionStoresScreen_addStore_title).lowercase(),
        body = stringResource(MR.strings.extensionStoresScreen_addStoreDeeplink_bodyText).lowercase(),
        onDismissRequest = onDismissRequest,
        confirmLabel = stringResource(
            if (processing) {
                MR.strings.extensionStoresScreen_addStore_processing
            } else {
                MR.strings.action_add
            },
        ).lowercase(),
        onConfirm = {
            if (!storeAlreadyExists && !processing) {
                onCreate()
            }
        },
        dismissLabel = stringResource(MR.strings.action_cancel).lowercase(),
        content = {
            MetroTextBox(
                value = storeIndexUrl,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = false,
            )
            val support = when {
                storeAlreadyExists -> stringResource(MR.strings.extensionStoresScreen_addStore_alreadyExists)
                errorMessage != null -> errorMessage
                else -> null
            }
            if (support != null) {
                MetroText(
                    text = support.lowercase(),
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.accent,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
    )
}
