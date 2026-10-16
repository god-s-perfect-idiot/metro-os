package mihon.feature.migration.config

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import com.metro.ui.metroNavBarPadding
import eu.kanade.core.util.fastFilterNot
import eu.kanade.domain.source.service.SourcePreferences
import mihon.domain.migration.models.MigrationFlag
import mihon.feature.common.utils.getLabel
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.preference.toggle
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun MigrationConfigScreenSheet(
    preferences: SourcePreferences,
    onDismissRequest: () -> Unit,
    onStartMigration: (extraSearchQuery: String?) -> Unit,
) {
    var extraSearchQuery by rememberSaveable { mutableStateOf("") }
    val migrationFlags by preferences.migrationFlags.collectAsState()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
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
                        pageTitle = stringResource(MR.strings.label_migration).lowercase(),
                        appTitle = "metron",
                    )
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            bottom = MetroAppBarDefaults.BarHeight + 24.dp,
                        ),
                    ) {
                        item(key = "flags-header") {
                            MetroText(
                                text = stringResource(
                                    MR.strings.migrationConfigScreen_dataToMigrateHeader,
                                ).lowercase(),
                                style = MetroTextStyle.SectionHeader,
                                color = MetroTheme.colors.primaryText,
                                modifier = Modifier.padding(
                                    start = MetroDimens.ScreenHorizontalMargin,
                                    end = MetroDimens.ScreenHorizontalMargin,
                                    top = 4.dp,
                                    bottom = 8.dp,
                                ),
                            )
                        }
                        item(key = "flags") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                MigrationFlag.entries
                                    .fastFilterNot { it == MigrationFlag.REMOVE_DOWNLOAD }
                                    .fastForEach { flag ->
                                        val selected = flag in migrationFlags
                                        MigrationFlagTag(
                                            text = stringResource(flag.getLabel()).lowercase(),
                                            selected = selected,
                                            onClick = {
                                                preferences.migrationFlags.getAndSet { currentFlags ->
                                                    if (flag in currentFlags) {
                                                        currentFlags - flag
                                                    } else {
                                                        currentFlags + flag
                                                    }
                                                }
                                            },
                                        )
                                    }
                            }
                        }
                        item(key = "remove-downloads") {
                            val removeDownloads = MigrationFlag.REMOVE_DOWNLOAD in migrationFlags
                            MetroToggleSwitch(
                                checked = removeDownloads,
                                onCheckedChange = {
                                    preferences.migrationFlags.getAndSet {
                                        if (removeDownloads) {
                                            it - MigrationFlag.REMOVE_DOWNLOAD
                                        } else {
                                            it + MigrationFlag.REMOVE_DOWNLOAD
                                        }
                                    }
                                },
                                label = stringResource(
                                    MR.strings.migrationConfigScreen_removeDownloadsTitle,
                                ).lowercase(),
                                modifier = Modifier.padding(
                                    horizontal = MetroDimens.ScreenHorizontalMargin,
                                    vertical = 8.dp,
                                ),
                            )
                        }
                        item(key = "search-query") {
                            MetroTextBox(
                                value = extraSearchQuery,
                                onValueChange = { extraSearchQuery = it },
                                placeholder = stringResource(
                                    MR.strings.migrationConfigScreen_additionalSearchQueryLabel,
                                ).lowercase(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = MetroDimens.ScreenHorizontalMargin,
                                        vertical = 8.dp,
                                    ),
                            )
                            MetroText(
                                text = stringResource(
                                    MR.strings.migrationConfigScreen_additionalSearchQuerySupportingText,
                                ).lowercase(),
                                style = MetroTextStyle.ListItemSubtitle,
                                color = MetroTheme.colors.secondaryText,
                                modifier = Modifier.padding(
                                    start = MetroDimens.ScreenHorizontalMargin,
                                    end = MetroDimens.ScreenHorizontalMargin,
                                    bottom = 8.dp,
                                ),
                            )
                        }
                        item(key = "hide-unmatched") {
                            MigrationSwitchRow(
                                title = stringResource(
                                    MR.strings.migrationConfigScreen_hideUnmatchedTitle,
                                ).lowercase(),
                                subtitle = null,
                                preference = preferences.migrationHideUnmatched,
                            )
                        }
                        item(key = "hide-without-updates") {
                            MigrationSwitchRow(
                                title = stringResource(
                                    MR.strings.migrationConfigScreen_hideWithoutUpdatesTitle,
                                ).lowercase(),
                                subtitle = stringResource(
                                    MR.strings.migrationConfigScreen_hideWithoutUpdatesSubtitle,
                                ).lowercase(),
                                preference = preferences.migrationHideWithoutUpdates,
                            )
                        }
                        item(key = "warning") {
                            MetroText(
                                text = stringResource(
                                    MR.strings.migrationConfigScreen_enhancedOptionsWarning,
                                ).lowercase(),
                                style = MetroTextStyle.ListItemSubtitle,
                                color = MetroTheme.colors.accent,
                                modifier = Modifier.padding(
                                    horizontal = MetroDimens.ScreenHorizontalMargin,
                                    vertical = 12.dp,
                                ),
                            )
                        }
                        item(key = "deep-search") {
                            MigrationSwitchRow(
                                title = stringResource(
                                    MR.strings.migrationConfigScreen_deepSearchModeTitle,
                                ).lowercase(),
                                subtitle = stringResource(
                                    MR.strings.migrationConfigScreen_deepSearchModeSubtitle,
                                ).lowercase(),
                                preference = preferences.migrationDeepSearchMode,
                            )
                        }
                        item(key = "prioritize-chapters") {
                            MigrationSwitchRow(
                                title = stringResource(
                                    MR.strings.migrationConfigScreen_prioritizeByChaptersTitle,
                                ).lowercase(),
                                subtitle = stringResource(
                                    MR.strings.migrationConfigScreen_prioritizeByChaptersSubtitle,
                                ).lowercase(),
                                preference = preferences.migrationPrioritizeByChapters,
                            )
                        }
                    }
                }

                MetroAppBar(
                    icons = listOf(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Forward,
                            label = stringResource(
                                MR.strings.migrationConfigScreen_continueButtonText,
                            ).lowercase(),
                            onClick = {
                                val cleaned = extraSearchQuery.trim().ifBlank { null }
                                onStartMigration(cleaned)
                            },
                        ),
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Close,
                            label = stringResource(MR.strings.action_cancel).lowercase(),
                            onClick = onDismissRequest,
                        ),
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun MigrationSwitchRow(
    title: String,
    subtitle: String?,
    preference: Preference<Boolean>,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MetroToggleSwitch(
            checked = preference.collectAsState().value,
            onCheckedChange = { preference.toggle() },
            label = title,
            modifier = Modifier.padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 8.dp,
            ),
        )
        if (subtitle != null) {
            MetroText(
                text = subtitle,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    end = MetroDimens.ScreenHorizontalMargin,
                    bottom = 8.dp,
                ),
            )
        }
    }
}

@Composable
private fun MigrationFlagTag(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = MetroTheme.colors.accent
    val border = if (selected) accent else MetroTheme.colors.primaryText
    val fill = if (selected) accent else Color.Transparent
    val label = if (selected) {
        MetroColors.tileContentColor(accent)
    } else {
        MetroTheme.colors.primaryText
    }
    Box(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .background(fill, RectangleShape)
            .border(2.dp, border, RectangleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        MetroText(
            text = text,
            style = MetroTextStyle.Body,
            color = label,
            maxLines = 1,
        )
    }
}
