package eu.kanade.presentation.more.settings.screen.data

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroMultiSelectDefaults
import com.metro.ui.MetroMultiSelectRow
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.backup.BackupFileValidator
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreWorker
import eu.kanade.tachiyomi.data.backup.restore.RestoreOptions
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

class RestoreBackupScreen(
    private val uri: String,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel =
            assistedMetroViewModel<RestoreBackupViewModel, RestoreBackupViewModel.Factory> { create(uri = uri) }
        val state by viewModel.state.collectAsState()
        val canRestore = state.canRestore && state.options.canRestore()

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
                        pageTitle = stringResource(MR.strings.pref_restore_backup).lowercase(),
                        appTitle = "metron",
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 4.dp,
                            bottom = MetroMultiSelectDefaults.ListBottomPadding,
                        ),
                    ) {
                        if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                            item(key = "miui-warning") {
                                MetroText(
                                    text = stringResource(MR.strings.restore_miui_warning),
                                    style = MetroTextStyle.Body,
                                    color = MetroTheme.colors.accent,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }

                        if (state.canRestore) {
                            items(
                                count = RestoreOptions.options.size,
                                key = { index -> index },
                            ) { index ->
                                val option = RestoreOptions.options[index]
                                val checked = option.getter(state.options)
                                MetroMultiSelectRow(
                                    title = stringResource(option.label).lowercase(),
                                    checked = checked,
                                    onClick = {
                                        viewModel.toggle(option.setter, !checked)
                                    },
                                )
                            }
                        }

                        if (state.error != null) {
                            item(key = "error") {
                                RestoreErrorMessage(error = state.error)
                            }
                        }
                    }
                }

                MetroAppBar(
                    icons = listOf(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Check,
                            label = stringResource(MR.strings.action_restore).lowercase(),
                            enabled = canRestore,
                            onClick = {
                                viewModel.startRestore()
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
}

@Composable
private fun RestoreErrorMessage(error: Any?) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (error) {
            is MissingRestoreComponents -> {
                MetroText(
                    text = stringResource(MR.strings.backup_restore_content_full),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.primaryText,
                )
                if (error.sources.isNotEmpty()) {
                    MetroText(
                        text = stringResource(MR.strings.backup_restore_missing_sources),
                        style = MetroTextStyle.ListItemTitle,
                        color = MetroTheme.colors.primaryText,
                    )
                    SelectionContainer {
                        MetroText(
                            text = error.sources.joinToString(
                                separator = "\n- ",
                                prefix = "- ",
                            ),
                            style = MetroTextStyle.Body,
                            color = MetroTheme.colors.secondaryText,
                        )
                    }
                }
                if (error.trackers.isNotEmpty()) {
                    MetroText(
                        text = stringResource(MR.strings.backup_restore_missing_trackers),
                        style = MetroTextStyle.ListItemTitle,
                        color = MetroTheme.colors.primaryText,
                    )
                    SelectionContainer {
                        MetroText(
                            text = error.trackers.joinToString(
                                separator = "\n- ",
                                prefix = "- ",
                            ),
                            style = MetroTextStyle.Body,
                            color = MetroTheme.colors.secondaryText,
                        )
                    }
                }
            }

            is InvalidRestore -> {
                MetroText(
                    text = stringResource(MR.strings.invalid_backup_file),
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.accent,
                )
                SelectionContainer {
                    MetroText(
                        text = error.uri?.toString().orEmpty(),
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
                MetroText(
                    text = stringResource(MR.strings.invalid_backup_file_error),
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.primaryText,
                )
                SelectionContainer {
                    MetroText(
                        text = error.message,
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
            }

            else -> {
                SelectionContainer {
                    MetroText(
                        text = error.toString(),
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
            }
        }
    }
}

@AssistedInject
class RestoreBackupViewModel(
    @Assisted private val uri: String,
    private val backupFileValidator: BackupFileValidator,
    private val context: Context,
) : ViewModel() {

    val state: StateFlow<RestoreBackupViewModel.State>
        field = MutableStateFlow<RestoreBackupViewModel.State>(State())

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(uri: String): RestoreBackupViewModel
    }

    init {
        viewModelScope.launchIO {
            validate(uri.toUri())
        }
    }

    fun toggle(setter: (RestoreOptions, Boolean) -> RestoreOptions, enabled: Boolean) {
        state.update {
            it.copy(
                options = setter(it.options, enabled),
            )
        }
    }

    fun startRestore() {
        BackupRestoreWorker.start(
            workManager = context.workManager,
            uri = uri.toUri(),
            options = state.value.options,
        )
    }

    private suspend fun validate(uri: Uri) {
        val results = try {
            backupFileValidator.validate(uri)
        } catch (e: Exception) {
            setError(
                error = InvalidRestore(uri, e.message.toString()),
                canRestore = false,
            )
            return
        }

        if (results.missingSources.isNotEmpty() || results.missingTrackers.isNotEmpty()) {
            setError(
                error = MissingRestoreComponents(uri, results.missingSources, results.missingTrackers),
                canRestore = true,
            )
            return
        }

        setError(error = null, canRestore = true)
    }

    private fun setError(error: Any?, canRestore: Boolean) {
        state.update {
            it.copy(
                error = error,
                canRestore = canRestore,
            )
        }
    }

    @Immutable
    data class State(
        val error: Any? = null,
        val canRestore: Boolean = false,
        val options: RestoreOptions = RestoreOptions(),
    )
}

private data class MissingRestoreComponents(
    val uri: Uri,
    val sources: List<String>,
    val trackers: List<String>,
)

private data class InvalidRestore(
    val uri: Uri? = null,
    val message: String,
)
