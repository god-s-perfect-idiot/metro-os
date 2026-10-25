package com.metro.people.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.metro.people.data.PeoplePane
import com.metro.ui.LocalMetroSubpageExit
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroColors
import com.metro.ui.MetroJumpList
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToastHost
import com.metro.ui.metroNavBarPadding
import com.metro.ui.rememberMetroToastController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SearchFieldRowHeight = 48.dp
private val SearchFieldBorderWidth = 3.dp
private val SearchFieldHorizontalPadding = 10.dp
private val SearchFieldBottomSpacing = 8.dp

@Composable
fun PeopleShell(
    state: PeopleState,
    onRequestWriteContacts: () -> Unit,
    onRequestCallLogPermission: () -> Unit = {},
    onRequestNotificationAccess: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observeState = generation

    var scrollToLetter by remember { mutableStateOf<Char?>(null) }
    var hubIntroPlayed by remember { mutableStateOf(false) }
    val searching = state.searchVisible
    val scope = rememberCoroutineScope()
    val toast = rememberMetroToastController()
    val hubPagerState = rememberPagerState(
        initialPage = PeoplePane.WhatsNew.ordinal,
        pageCount = { PeoplePane.entries.size },
    )

    LaunchedEffect(state.toastMessage) {
        val message = state.toastMessage ?: return@LaunchedEffect
        toast.show(message)
        state.consumeToast()
    }

    fun persistContact(draft: com.metro.people.data.ContactDraft, onFinished: () -> Unit) {
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    state.saveNewContact(draft)
                }
                when (result) {
                    PeopleState.SaveContactResult.Saved -> {
                        state.reloadContacts()
                        state.showToast(R.string.contact_saved)
                        if (state.route is PeopleRoute.NewContact) {
                            state.closeOverlay()
                        }
                    }
                    PeopleState.SaveContactResult.NeedsIdentity -> {
                        state.showToast(R.string.contact_needs_identity)
                    }
                    PeopleState.SaveContactResult.NeedsWritePermission -> {
                        state.showToast(R.string.contact_needs_write)
                        onRequestWriteContacts()
                    }
                    PeopleState.SaveContactResult.Failed -> {
                        state.showToast(R.string.contact_save_failed)
                    }
                }
            } finally {
                onFinished()
            }
        }
    }

    // After WRITE_CONTACTS is granted, finish a save that was waiting on permission.
    LaunchedEffect(state.hasWriteContactsPermission, state.generation) {
        if (!state.hasWriteContactsPermission) return@LaunchedEffect
        val pending = state.consumePendingSaveDraft() ?: return@LaunchedEffect
        persistContact(pending) {}
    }

    BackHandler(enabled = searching && state.route == PeopleRoute.Hub) {
        state.dismissSearch()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .metroNavBarPadding()
            .background(Color.Black),
    ) {
        MetroSubpageHost(
            route = state.route,
            isRoot = { it == PeopleRoute.Hub },
            parentOf = { PeopleRoute.Hub },
            loadKeyOf = { subpageLoadKey(it) },
            onGoBack = state::closeOverlay,
            modifier = Modifier.fillMaxSize(),
            rootContent = {
                if (searching) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        MetroAppTitle(title = stringResource(R.string.people_search))
                        ContactSearchBar(
                            query = state.searchQuery,
                            onQueryChange = state::updateSearchQuery,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = SearchFieldBottomSpacing,
                            ),
                        )
                        AllPane(
                            filterLabel = state.filterLabel,
                            grouped = state.groupedContacts,
                            flatContacts = state.visibleContacts,
                            isLoading = state.contactsLoading,
                            searchActive = true,
                            onFilterClick = state::openFilter,
                            onJumpClick = state::toggleJumpList,
                            onOpenDetail = { state.openDetail(it.id) },
                            onAddToSpeedDial = state::addToSpeedDial,
                            onPinToStart = state::pinToStart,
                            onDeleteContact = state::deleteContact,
                            scrollToLetter = scrollToLetter,
                            onScrollConsumed = { scrollToLetter = null },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    PeopleHub(
                        state = state,
                        pagerState = hubPagerState,
                        scrollToLetter = scrollToLetter,
                        onScrollConsumed = { scrollToLetter = null },
                        onRequestCallLogPermission = onRequestCallLogPermission,
                        onRequestNotificationAccess = onRequestNotificationAccess,
                        skipIntro = hubIntroPlayed,
                        onIntroPlayed = { hubIntroPlayed = true },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
            subpageContent = { route ->
                val requestExit = LocalMetroSubpageExit.current
                val onBack = { requestExit?.invoke() }
                when (route) {
                    PeopleRoute.Filter -> FilterScreen(
                        initial = state.filter,
                        accounts = state.knownAccounts(),
                        onSave = { filter ->
                            state.saveFilter(filter)
                            onBack()
                        },
                        onCancel = { onBack() },
                    )
                    PeopleRoute.Accounts -> AccountsScreen(
                        options = state.accountOptions,
                        onBack = { onBack() },
                        onSelect = {
                            state.showExternalStub("${it.label} account setup not available in v1")
                            onBack()
                        },
                    )
                    PeopleRoute.NewContact -> {
                        NewContactScreen(
                            prefill = state.newContactPrefill,
                            accounts = state.saveAccounts,
                            onSave = { draft, onFinished ->
                                persistContact(draft, onFinished)
                            },
                            onCancel = { onBack() },
                        )
                    }
                    is PeopleRoute.Detail -> {
                        state.selectedDetail?.let { detail ->
                            ContactDetailScreen(
                                detail = detail,
                                onBack = { onBack() },
                                onCall = { state.callContact(detail.summary) },
                                onText = { state.textContact(detail.summary) },
                                onWhatsAppCall = {
                                    detail.whatsApp?.let(state::whatsAppCall)
                                },
                                onWhatsAppText = {
                                    detail.whatsApp?.let(state::whatsAppText)
                                },
                                onEmail = state::emailContact,
                            )
                        }
                    }
                    PeopleRoute.Hub -> Unit
                }
            },
        )

        val appBarVisible = state.route == PeopleRoute.Hub && !searching
        MetroAppBar(
            visible = appBarVisible,
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Add,
                    label = "new",
                    onClick = { state.openNewContact() },
                    contentDescription = stringResource(R.string.new_contact),
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Search,
                    label = "search",
                    onClick = state::openSearch,
                    contentDescription = stringResource(R.string.search_contacts),
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (state.jumpListVisible && !searching && state.route == PeopleRoute.Hub) {
            MetroJumpList(
                activeLetters = state.groupedContacts.keys,
                onLetterSelected = { scrollToLetter = it },
                onDismiss = state::dismissJumpList,
            )
        }

        MetroToastHost(
            controller = toast,
            modifier = Modifier.align(Alignment.TopCenter),
        )
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

private fun subpageLoadKey(route: PeopleRoute): Any = when (route) {
    PeopleRoute.Filter -> "Filter"
    PeopleRoute.Accounts -> "Accounts"
    PeopleRoute.NewContact -> "NewContact"
    is PeopleRoute.Detail -> "Detail:${route.contactId}"
    PeopleRoute.Hub -> "Hub"
}
