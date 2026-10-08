package eu.kanade.presentation.category

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import eu.kanade.tachiyomi.ui.category.CategoryScreenState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun CategoryScreen(
    state: CategoryScreenState.Success,
    onClickCreate: () -> Unit,
    onRename: (Category, String) -> Unit,
    onClickDelete: (Category) -> Unit,
    onChangeOrder: (Category, Int) -> Unit,
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
                    pageTitle = stringResource(MR.strings.action_edit_categories).lowercase(),
                    appTitle = "metron",
                )
                if (state.isEmpty) {
                    MetroEmptyState(
                        message = stringResource(MR.strings.information_empty_category),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = MetroAppBarDefaults.BarHeight),
                    )
                } else {
                    CategoryContent(
                        categories = state.categories,
                        lazyListState = lazyListState,
                        onRename = onRename,
                        onClickDelete = onClickDelete,
                        onChangeOrder = onChangeOrder,
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
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun CategoryContent(
    categories: List<Category>,
    lazyListState: LazyListState,
    onRename: (Category, String) -> Unit,
    onClickDelete: (Category) -> Unit,
    onChangeOrder: (Category, Int) -> Unit,
) {
    val categoriesState = remember { categories.toMutableStateList() }
    val contentPadding = PaddingValues(bottom = MetroAppBarDefaults.BarHeight + 32.dp)
    val reorderableState = rememberReorderableLazyListState(lazyListState, contentPadding) { from, to ->
        val item = categoriesState.removeAt(from.index)
        categoriesState.add(to.index, item)
        onChangeOrder(item, to.index)
    }

    LaunchedEffect(categories) {
        if (!reorderableState.isAnyItemDragging) {
            categoriesState.clear()
            categoriesState.addAll(categories)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = contentPadding,
    ) {
        items(
            items = categoriesState,
            key = { category -> category.key },
        ) { category ->
            ReorderableItem(reorderableState, category.key) {
                CategoryEditableRow(
                    category = category,
                    otherNames = categoriesState.filter { it.id != category.id }.map { it.name },
                    onRename = onRename,
                    onClickDelete = { onClickDelete(category) },
                    dragHandleModifier = Modifier.draggableHandle(),
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun CategoryEditableRow(
    category: Category,
    otherNames: List<String>,
    onRename: (Category, String) -> Unit,
    onClickDelete: () -> Unit,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var draft by remember(category.id, category.name) { mutableStateOf(category.name) }

    fun commitIfValid() {
        val trimmed = draft.trim()
        if (trimmed.isEmpty() || trimmed == category.name || otherNames.contains(trimmed)) {
            draft = category.name
            return
        }
        onRename(category, trimmed)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryDragHandle(modifier = dragHandleModifier.padding(end = 8.dp))
        MetroTextBox(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        commitIfValid()
                    }
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    commitIfValid()
                    focusManager.clearFocus()
                },
            ),
        )
        MetroCircleIconButton(
            type = MetroSystemIconType.Delete,
            contentDescription = stringResource(MR.strings.action_delete),
            onClick = onClickDelete,
            size = 36.dp,
            iconSize = 18.dp,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** Three-line grip — drag handle for category reorder. */
@Composable
private fun CategoryDragHandle(
    modifier: Modifier = Modifier,
) {
    val color = MetroTheme.colors.secondaryText
    Canvas(modifier = modifier.size(28.dp)) {
        val stroke = size.minDimension * 0.08f
        val left = size.width * 0.18f
        val right = size.width * 0.82f
        val ys = listOf(0.30f, 0.50f, 0.70f)
        ys.forEach { yFrac ->
            val y = size.height * yFrac
            drawLine(
                color = color,
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = stroke,
                cap = StrokeCap.Butt,
            )
        }
    }
}

private val Category.key: String
    get() = "category-${id}"
