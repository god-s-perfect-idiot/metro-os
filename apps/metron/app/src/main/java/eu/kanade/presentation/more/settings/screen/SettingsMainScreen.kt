package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroListItem
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen

private val ContentBottomClearance = MetroAppBarDefaults.BarHeight + 32.dp

object SettingsMainScreen : Screen() {

    @Composable
    override fun Content() {
        Content(twoPane = false)
    }

    @Composable
    fun Content(twoPane: Boolean) {
        val navigator = LocalNavigator.currentOrThrow
        val state = rememberLazyListState()
        val indexSelected = if (twoPane) {
            items.indexOfFirst { it.screen::class == navigator.items.first()::class }
                .also {
                    LaunchedEffect(Unit) {
                        if (it >= 0) state.animateScrollToItem(it)
                    }
                }
        } else {
            null
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
                    MetroSettingsHeader(
                        pageTitle = stringResource(MR.strings.label_settings).lowercase(),
                        appTitle = "metron",
                    )
                    LazyColumn(
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = ContentBottomClearance),
                    ) {
                        itemsIndexed(
                            items = items,
                            key = { _, item -> item.hashCode() },
                        ) { index, item ->
                            val selected = indexSelected == index
                            MetroListItem(
                                title = stringResource(item.titleRes).lowercase(),
                                subtitle = item.formatSubtitle()?.lowercase(),
                                titleColor = if (selected) MetroTheme.colors.accent else null,
                                oneLineMinHeight = 64.dp,
                                twoLineMinHeight = 72.dp,
                                verticalPadding = 8.dp,
                                onClick = { navigator.navigate(item.screen, twoPane) },
                            )
                        }
                    }
                }

                MetroAppBar(
                    icons = listOf(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Search,
                            label = stringResource(MR.strings.action_search),
                            onClick = { navigator.navigate(SettingsSearchScreen(), twoPane) },
                        ),
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    private fun Navigator.navigate(screen: VoyagerScreen, twoPane: Boolean) {
        if (twoPane) replaceAll(screen) else push(screen)
    }

    private data class Item(
        val titleRes: StringResource,
        val subtitleRes: StringResource? = null,
        val formatSubtitle: @Composable () -> String? = { subtitleRes?.let { stringResource(it) } },
        val screen: VoyagerScreen,
    )

    private val items = listOf(
        Item(
            titleRes = MR.strings.pref_category_appearance,
            subtitleRes = MR.strings.pref_appearance_summary,
            screen = SettingsAppearanceScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_library,
            subtitleRes = MR.strings.pref_library_summary,
            screen = SettingsLibraryScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_reader,
            subtitleRes = MR.strings.pref_reader_summary,
            screen = SettingsReaderScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_downloads,
            subtitleRes = MR.strings.pref_downloads_summary,
            screen = SettingsDownloadScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_tracking,
            subtitleRes = MR.strings.pref_tracking_summary,
            screen = SettingsTrackingScreen,
        ),
        Item(
            titleRes = MR.strings.browse,
            subtitleRes = MR.strings.pref_browse_summary,
            screen = SettingsBrowseScreen,
        ),
        Item(
            titleRes = MR.strings.label_data_storage,
            subtitleRes = MR.strings.pref_backup_summary,
            screen = SettingsDataScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_security,
            subtitleRes = MR.strings.pref_security_summary,
            screen = SettingsSecurityScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_advanced,
            subtitleRes = MR.strings.pref_advanced_summary,
            screen = SettingsAdvancedScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_about,
            formatSubtitle = {
                "${stringResource(MR.strings.app_name)} ${AboutScreen.getVersionName(withBuildDate = false)}"
            },
            screen = AboutScreen,
        ),
    )
}
