package eu.kanade.presentation.category.components

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
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroMultiSelectRow
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import eu.kanade.presentation.category.visualName
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun CategoryCreateDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
    categories: List<String>,
) {
    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val nameAlreadyExists = remember(name) { categories.contains(name) }
    val canCreate = name.isNotEmpty() && !nameAlreadyExists

    MetroMessageDialog(
        title = stringResource(MR.strings.action_add_category).lowercase(),
        onDismissRequest = onDismissRequest,
        confirmLabel = stringResource(MR.strings.action_add).lowercase(),
        confirmEnabled = canCreate,
        onConfirm = {
            onCreate(name)
            onDismissRequest()
        },
        dismissLabel = stringResource(MR.strings.action_cancel).lowercase(),
        content = {
            MetroTextBox(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = stringResource(MR.strings.name).lowercase(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (canCreate) {
                            onCreate(name)
                            onDismissRequest()
                        }
                    },
                ),
            )
            if (name.isNotEmpty() && nameAlreadyExists) {
                MetroText(
                    text = stringResource(MR.strings.error_category_exists).lowercase(),
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.accent,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
    )

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}

@Composable
fun CategoryDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    category: String,
) {
    MetroMessageDialog(
        title = stringResource(MR.strings.delete_category).lowercase(),
        body = stringResource(MR.strings.delete_category_confirmation, category).lowercase(),
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
fun ChangeCategoryDialog(
    initialSelection: List<CheckboxState<Category>>,
    onDismissRequest: () -> Unit,
    onEditCategories: () -> Unit,
    onConfirm: (List<Long>, List<Long>) -> Unit,
) {
    if (initialSelection.isEmpty()) {
        MetroMessageDialog(
            title = stringResource(MR.strings.action_move_category).lowercase(),
            body = stringResource(MR.strings.information_empty_category_dialog).lowercase(),
            onDismissRequest = onDismissRequest,
            confirmLabel = stringResource(MR.strings.action_edit_categories).lowercase(),
            onConfirm = {
                onDismissRequest()
                onEditCategories()
            },
            dismissLabel = stringResource(MR.strings.action_cancel).lowercase(),
        )
        return
    }

    var selection by remember { mutableStateOf(initialSelection) }

    MetroMessageDialog(
        title = stringResource(MR.strings.action_move_category).lowercase(),
        onDismissRequest = onDismissRequest,
        confirmLabel = stringResource(MR.strings.action_ok).lowercase(),
        onConfirm = {
            onDismissRequest()
            onConfirm(
                selection
                    .filter { it is CheckboxState.State.Checked || it is CheckboxState.TriState.Include }
                    .map { it.value.id },
                selection
                    .filter { it is CheckboxState.State.None || it is CheckboxState.TriState.None }
                    .map { it.value.id },
            )
        },
        dismissLabel = stringResource(MR.strings.action_cancel).lowercase(),
        neutralLabel = stringResource(MR.strings.action_edit).lowercase(),
        onNeutral = {
            onDismissRequest()
            onEditCategories()
        },
        content = {
            selection.forEach { checkbox ->
                val checked = when (checkbox) {
                    is CheckboxState.State.Checked -> true
                    is CheckboxState.TriState.Include -> true
                    else -> false
                }
                MetroMultiSelectRow(
                    title = checkbox.value.visualName.lowercase(),
                    checked = checked,
                    onClick = {
                        val index = selection.indexOf(checkbox)
                        if (index != -1) {
                            val mutableList = selection.toMutableList()
                            mutableList[index] = checkbox.next()
                            selection = mutableList.toList()
                        }
                    },
                )
            }
        },
    )
}
