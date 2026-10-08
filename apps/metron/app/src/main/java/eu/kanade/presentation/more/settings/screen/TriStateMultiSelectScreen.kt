package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroCheckBoxDefaults
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroMultiSelectDefaults
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Include / exclude / none picker — same chrome as [com.metro.ui.MetroMultiSelectList].
 * Tap cycles: none → include → exclude → none.
 */
class TriStateMultiSelectScreen(
    private val title: String,
    private val message: String?,
    private val items: List<Pair<String, String>>,
    private val initialIncluded: Set<String>,
    private val initialExcluded: Set<String>,
    private val onConfirm: (included: Set<String>, excluded: Set<String>) -> Unit,
) : Screen() {

    private enum class State { NONE, INCLUDE, EXCLUDE }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val states = remember {
            items.map { (id, _) ->
                when (id) {
                    in initialIncluded -> State.INCLUDE
                    in initialExcluded -> State.EXCLUDE
                    else -> State.NONE
                }
            }.toMutableStateList()
        }

        MetroSystemTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .metroNavBarPadding()
                    .background(MetroTheme.colors.background),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    MetroAppTitle(title = title.lowercase())
                    if (!message.isNullOrBlank()) {
                        MetroText(
                            text = message.lowercase(),
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText,
                            modifier = Modifier.padding(
                                horizontal = MetroDimens.ScreenHorizontalMargin,
                                vertical = 8.dp,
                            ),
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 12.dp,
                            bottom = MetroMultiSelectDefaults.ListBottomPadding,
                        ),
                    ) {
                        itemsIndexed(items, key = { _, item -> item.first }) { index, item ->
                            val state = states[index]
                            TriStateRow(
                                title = item.second.lowercase(),
                                state = state,
                                onClick = {
                                    states[index] = when (state) {
                                        State.NONE -> State.INCLUDE
                                        State.INCLUDE -> State.EXCLUDE
                                        State.EXCLUDE -> State.NONE
                                    }
                                },
                            )
                        }
                    }
                }
                MetroAppBar(
                    icons = listOf(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Check,
                            label = stringResource(MR.strings.action_ok).lowercase(),
                            onClick = {
                                val included = items.mapIndexedNotNull { i, (id, _) ->
                                    id.takeIf { states[i] == State.INCLUDE }
                                }.toSet()
                                val excluded = items.mapIndexedNotNull { i, (id, _) ->
                                    id.takeIf { states[i] == State.EXCLUDE }
                                }.toSet()
                                onConfirm(included, excluded)
                                navigator.pop()
                            },
                        ),
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Close,
                            label = stringResource(MR.strings.action_cancel).lowercase(),
                            onClick = { navigator.pop() },
                        ),
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    @Composable
    private fun TriStateRow(
        title: String,
        state: State,
        onClick: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MetroMultiSelectDefaults.RowMinHeight)
                .clipToBounds()
                .metroClickable(onClick = onClick)
                .padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    end = MetroDimens.ScreenHorizontalMargin,
                    top = MetroMultiSelectDefaults.RowVerticalPadding,
                    bottom = MetroMultiSelectDefaults.RowVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            when (state) {
                State.NONE -> MetroCheckBox(
                    checked = false,
                    onCheckedChange = null,
                    size = MetroMultiSelectDefaults.CheckboxSize,
                )
                State.INCLUDE -> MetroCheckBox(
                    checked = true,
                    onCheckedChange = null,
                    size = MetroMultiSelectDefaults.CheckboxSize,
                )
                State.EXCLUDE -> ExcludeCheckBox(size = MetroMultiSelectDefaults.CheckboxSize)
            }
            MetroText(
                text = title,
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.primaryText,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .padding(start = MetroMultiSelectDefaults.LeadingToTitleGap)
                    .weight(1f, fill = false),
            )
        }
    }

    @Composable
    private fun ExcludeCheckBox(size: Dp) {
        val accent = MetroTheme.colors.accent
        val glyph = MetroColors.DarkPrimaryText
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size),
        ) {
            Canvas(modifier = Modifier.size(size)) {
                drawRect(color = accent)
                val inset = this.size.minDimension * 0.28f
                val stroke = MetroCheckBoxDefaults.BorderWidth.toPx()
                drawLine(
                    color = glyph,
                    start = Offset(inset, inset),
                    end = Offset(this.size.width - inset, this.size.height - inset),
                    strokeWidth = stroke,
                )
                drawLine(
                    color = glyph,
                    start = Offset(this.size.width - inset, inset),
                    end = Offset(inset, this.size.height - inset),
                    strokeWidth = stroke,
                )
            }
        }
    }
}
