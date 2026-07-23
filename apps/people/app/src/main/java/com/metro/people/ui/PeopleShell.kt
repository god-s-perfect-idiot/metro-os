package com.metro.people.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.people.R
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroColors
import com.metro.ui.MetroJumpList
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import com.metro.ui.metroNavBarPadding

private val SearchFieldRowHeight = 48.dp
private val SearchFieldBorderWidth = 3.dp
private val SearchFieldHorizontalPadding = 10.dp
private val SearchFieldBottomSpacing = 8.dp

@Composable
fun PeopleShell(
    state: PeopleState,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observeState = generation

    var scrollToLetter by remember { mutableStateOf<Char?>(null) }
    val searching = state.searchVisible

    BackHandler(enabled = searching) {
        state.dismissSearch()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .metroNavBarPadding()
            .background(Color.Black),
    ) {
        when (state.route) {
            PeopleRoute.Hub -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    MetroAppTitle(
                        title = stringResource(
                            if (searching) R.string.people_search else R.string.app_name,
                        ),
                    )
                    if (searching) {
                        ContactSearchBar(
                            query = state.searchQuery,
                            onQueryChange = state::updateSearchQuery,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = SearchFieldBottomSpacing,
                            ),
                        )
                    } else {
                        MetroText(
                            text = "all",
                            style = MetroTextStyle.HubTitle,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(top = 4.dp, bottom = 12.dp),
                        )
                    }
                    AllPane(
                        filterLabel = state.filterLabel,
                        grouped = state.groupedContacts,
                        flatContacts = state.visibleContacts,
                        searchActive = searching,
                        onFilterClick = state::openFilter,
                        onJumpClick = state::toggleJumpList,
                        onOpenDetail = { state.openDetail(it.id) },
                        onAddToSpeedDial = state::addToSpeedDial,
                        onPinToStart = state::pinToStart,
                        scrollToLetter = scrollToLetter,
                        onScrollConsumed = { scrollToLetter = null },
                        modifier = Modifier.padding(
                            bottom = if (searching) 0.dp else MetroAppBarDefaults.BarHeight,
                        ),
                    )
                }

                AnimatedVisibility(
                    visible = !searching,
                    enter = slideInVertically(
                        animationSpec = tween(MetroTransitions.AppBarSlideMs),
                        initialOffsetY = { it },
                    ) + fadeIn(animationSpec = tween(MetroTransitions.AppBarSlideMs)),
                    exit = slideOutVertically(
                        animationSpec = tween(MetroTransitions.AppBarSlideMs),
                        targetOffsetY = { it },
                    ) + fadeOut(animationSpec = tween(MetroTransitions.AppBarSlideMs)),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    MetroAppBar(
                        icons = listOf(
                            MetroAppBarIcon(
                                type = MetroSystemIconType.Search,
                                label = "search",
                                onClick = state::openSearch,
                                contentDescription = stringResource(R.string.search_contacts),
                            ),
                        ),
                    )
                }
            }
            PeopleRoute.Filter -> FilterScreen(
                initial = state.filter,
                accounts = state.knownAccounts(),
                onSave = state::saveFilter,
                onCancel = state::closeOverlay,
            )
            PeopleRoute.Accounts -> AccountsScreen(
                options = state.accountOptions,
                onBack = state::closeOverlay,
                onSelect = {
                    state.showExternalStub("${it.label} account setup not available in v1")
                    state.closeOverlay()
                },
            )
            is PeopleRoute.Detail -> {
                state.selectedDetail?.let { detail ->
                    ContactDetailScreen(
                        detail = detail,
                        onBack = state::closeOverlay,
                        onCall = { state.callContact(detail.summary) },
                        onText = { state.textContact(detail.summary) },
                        onEmail = state::emailContact,
                    )
                }
            }
        }

        if (state.jumpListVisible && !searching) {
            MetroJumpList(
                activeLetters = state.groupedContacts.keys,
                onLetterSelected = { scrollToLetter = it },
                onDismiss = state::dismissJumpList,
            )
        }
    }
}

@Composable
private fun ContactSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val accent = MetroTheme.colors.accent

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // WP8.1 messaging/people search: white fill, accent border, black text at top of page.
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(SearchFieldRowHeight)
            .background(MetroColors.LightBackground, RectangleShape)
            .border(SearchFieldBorderWidth, accent, RectangleShape)
            .focusRequester(focusRequester)
            .padding(horizontal = SearchFieldHorizontalPadding),
        textStyle = MetroTextStyle.ListItemTitle.toTextStyle().copy(
            color = MetroColors.LightPrimaryText,
        ),
        cursorBrush = SolidColor(accent),
        singleLine = true,
        decorationBox = { inner ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    MetroText(
                        text = stringResource(R.string.search_contacts),
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroColors.LightPrimaryText.copy(alpha = 0.5f),
                    )
                }
                inner()
            }
        },
    )
}
