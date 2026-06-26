package com.metro.people.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.metroNavBarPadding

@Composable
fun PeopleShell(
    state: PeopleState,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observeState = generation

    var scrollToLetter by remember { mutableStateOf<Char?>(null) }

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
                    MetroText(
                        text = "all",
                        style = MetroTextStyle.HubTitle,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(top = 8.dp, bottom = 12.dp),
                    )
                    AllPane(
                        filterLabel = state.filterLabel,
                        grouped = state.groupedContacts,
                        onFilterClick = state::openFilter,
                        onJumpClick = state::toggleJumpList,
                        onOpenDetail = { state.openDetail(it.id) },
                        scrollToLetter = scrollToLetter,
                        onScrollConsumed = { scrollToLetter = null },
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

        if (state.jumpListVisible) {
            JumpListOverlay(
                letters = state.jumpLetters,
                onLetterSelected = { scrollToLetter = it },
                onDismiss = state::dismissJumpList,
            )
        }
    }
}
