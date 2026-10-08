package eu.kanade.presentation.browse

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.DisplayMetrics
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroListItem
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.extension.interactor.ExtensionSourceItem
import eu.kanade.presentation.browse.components.ExtensionIcon
import eu.kanade.presentation.browse.components.label
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsViewModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private val ContentBottomClearance = MetroAppBarDefaults.BarHeight + 32.dp

@Composable
fun ExtensionDetailsScreen(
    navigateUp: () -> Unit,
    state: ExtensionDetailsViewModel.State.Success,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickEnableAll: () -> Unit,
    onClickDisableAll: () -> Unit,
    onClickClearCookies: () -> Unit,
    onClickUninstall: () -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    onClickIncognito: (Boolean) -> Unit,
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedNavigateUp = navigateUp
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    var showContentWarning by remember { mutableStateOf(false) }
    val contentWarning = state.extension.contentWarning.label

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
                    pageTitle = stringResource(MR.strings.label_extension_info).lowercase(),
                    appTitle = "metron",
                )
                ExtensionDetailsBody(
                    extension = state.extension,
                    sources = state.sources,
                    incognitoMode = state.isIncognito,
                    onClickStore = state.extension.store
                        ?.contact
                        ?.website
                        ?.takeIf { it.isNotBlank() }
                        ?.let { website -> { uriHandler.openUri(website) } },
                    onClickSourcePreferences = onClickSourcePreferences,
                    onClickUninstall = onClickUninstall,
                    onClickSource = onClickSource,
                    onClickIncognito = onClickIncognito,
                    onClickContentWarning = { showContentWarning = true },
                    onClickAppInfo = {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", state.extension.pkgName, null)
                            context.startActivity(this)
                        }
                        Unit
                    }.takeIf { state.extension.isShared },
                    onCopyDebug = {
                        val extension = state.extension
                        val extDebugInfo = buildString {
                            append(
                                """
                                Extension name: ${extension.name} (lang: ${extension.lang}; package: ${extension.pkgName})
                                Extension version: ${extension.versionName} (lib: ${extension.libVersion}; version code: ${extension.versionCode})
                                Content warning: ${extension.contentWarning}
                                """.trimIndent(),
                            )
                            append("\n\n")
                            appendLine(
                                """
                                Update available: ${extension.hasUpdate}
                                Orphaned: ${extension.isObsolete}
                                Shared: ${extension.isShared}
                                """.trimIndent(),
                            )
                            val store = extension.store
                            if (store != null) {
                                append("Repository: ${store.indexUrl}")
                            }
                        }
                        context.copyToClipboard("Extension Debug information", extDebugInfo)
                    },
                )
            }

            MetroAppBar(
                menuItems = listOf(
                    MetroAppBarMenuItem(
                        text = stringResource(MR.strings.action_enable_all).lowercase(),
                        onClick = onClickEnableAll,
                    ),
                    MetroAppBarMenuItem(
                        text = stringResource(MR.strings.action_disable_all).lowercase(),
                        onClick = onClickDisableAll,
                    ),
                    MetroAppBarMenuItem(
                        text = stringResource(MR.strings.pref_clear_cookies).lowercase(),
                        onClick = onClickClearCookies,
                    ),
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showContentWarning && contentWarning != null) {
        ContentWarningDialog(
            label = contentWarning.title,
            description = contentWarning.description,
            onClickConfirm = { showContentWarning = false },
        )
    }
}

@Composable
private fun ExtensionDetailsBody(
    extension: Extension.Loaded,
    sources: List<ExtensionSourceItem>,
    incognitoMode: Boolean,
    onClickStore: (() -> Unit)?,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickUninstall: () -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    onClickIncognito: (Boolean) -> Unit,
    onClickContentWarning: () -> Unit,
    onClickAppInfo: (() -> Unit)?,
    onCopyDebug: () -> Unit,
) {
    val context = LocalContext.current
    val contentWarning = extension.contentWarning.label

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = ContentBottomClearance),
    ) {
        if (extension.isObsolete) {
            item(key = "obsolete") {
                MetroText(
                    text = stringResource(MR.strings.obsolete_extension_message),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.accent,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .metroClickable(onClick = onCopyDebug)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                ExtensionIcon(
                    modifier = Modifier
                        .size(96.dp)
                        .background(MetroTheme.colors.secondarySurface, RectangleShape),
                    extension = extension,
                    density = DisplayMetrics.DENSITY_XXXHIGH,
                )
                Spacer(modifier = Modifier.height(12.dp))
                MetroText(
                    text = extension.name,
                    style = MetroTextStyle.SectionHeader,
                    color = MetroTheme.colors.primaryText,
                )
                MetroText(
                    text = extension.pkgName,
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(top = 2.dp),
                )
                extension.store?.let { store ->
                    MetroText(
                        text = store.name.lowercase(),
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.accent,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .then(
                                if (onClickStore != null) {
                                    Modifier.metroClickable(onClick = onClickStore)
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
                if (!extension.isShared) {
                    MetroText(
                        text = stringResource(MR.strings.ext_installer_private).lowercase(),
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        item(key = "meta") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MetaColumn(
                    primary = extension.versionName,
                    secondary = stringResource(MR.strings.ext_info_version).lowercase(),
                    modifier = Modifier.weight(1f),
                )
                MetaColumn(
                    primary = LocaleHelper.getSourceDisplayName(extension.lang, context),
                    secondary = stringResource(MR.strings.ext_info_language).lowercase(),
                    modifier = Modifier.weight(1f),
                )
                if (contentWarning != null) {
                    MetaColumn(
                        primary = stringResource(contentWarning.title),
                        secondary = stringResource(MR.strings.ext_info_warning).lowercase(),
                        accent = true,
                        onClick = onClickContentWarning,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item(key = "actions") {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetroBorderButton(
                    text = stringResource(MR.strings.ext_uninstall).lowercase(),
                    onClick = onClickUninstall,
                )
                if (onClickAppInfo != null) {
                    MetroBorderButton(
                        text = stringResource(MR.strings.ext_app_info).lowercase(),
                        onClick = onClickAppInfo,
                    )
                }
            }
        }

        item(key = "incognito") {
            MetroToggleSwitch(
                checked = incognitoMode,
                onCheckedChange = onClickIncognito,
                label = stringResource(MR.strings.pref_incognito_mode).lowercase(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            MetroText(
                text = stringResource(MR.strings.pref_incognito_mode_extension_summary),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
            )
        }

        item(key = "sources-header") {
            MetroText(
                text = stringResource(MR.strings.label_sources).lowercase(),
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.primaryText,
                modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp),
            )
        }

        items(sources, key = { it.source.id }) { source ->
            val title = if (source.labelAsName) {
                source.source.toString()
            } else {
                LocaleHelper.getSourceDisplayName(source.source.lang, context)
            }
            MetroListItem(
                title = title.lowercase(),
                subtitle = if (source.enabled) {
                    stringResource(MR.strings.on).lowercase()
                } else {
                    stringResource(MR.strings.off).lowercase()
                },
                titleColor = if (source.enabled) null else MetroTheme.colors.secondaryText,
                trailing = {
                    if (source.source is ConfigurableSource) {
                        MetroText(
                            text = stringResource(MR.strings.label_settings).lowercase(),
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.accent,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .metroClickable {
                                    onClickSourcePreferences(source.source.id)
                                },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                onClick = { onClickSource(source.source.id) },
            )
        }
    }
}

@Composable
private fun MetaColumn(
    primary: String,
    secondary: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.then(
            if (onClick != null) Modifier.metroClickable(onClick = onClick) else Modifier,
        ),
    ) {
        MetroText(
            text = primary,
            style = MetroTextStyle.ListItemTitle,
            color = if (accent) MetroTheme.colors.accent else MetroTheme.colors.primaryText,
        )
        MetroText(
            text = secondary + if (onClick != null) " ⓘ" else "",
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun ContentWarningDialog(
    label: StringResource,
    description: StringResource,
    onClickConfirm: () -> Unit,
) {
    MetroMessageDialog(
        title = stringResource(label),
        body = stringResource(description),
        confirmLabel = stringResource(MR.strings.action_ok).lowercase(),
        onConfirm = onClickConfirm,
        onDismissRequest = onClickConfirm,
    )
}
