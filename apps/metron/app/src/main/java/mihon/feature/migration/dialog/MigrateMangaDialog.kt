package mihon.feature.migration.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.screen.Screen
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.domain.manga.model.hasCustomCover
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mihon.domain.migration.models.MigrationFlag
import mihon.domain.migration.usecases.MigrateMangaUseCase
import mihon.feature.common.utils.getLabel
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun Screen.MigrateMangaDialog(
    current: Manga,
    target: Manga,
    onClickTitle: () -> Unit,
    onDismissRequest: () -> Unit,
    onComplete: () -> Unit = onDismissRequest,
) {
    val scope = rememberCoroutineScope()

    val viewModel = metroViewModel<MigrateDialogViewModel>()
    LaunchedEffect(current, target) {
        viewModel.init(current, target)
    }
    val state by viewModel.state.collectAsState()

    if (state.isMigrated) return

    if (state.isMigrating) {
        MetroSystemTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .metroNavBarPadding()
                    .background(MetroTheme.colors.background.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                MetroLoadingDots()
            }
        }
        return
    }

    MetroMessageDialog(
        title = stringResource(MR.strings.migration_dialog_what_to_include).lowercase(),
        titleStyle = MetroTextStyle.DialogBody,
        onDismissRequest = onDismissRequest,
        confirmLabel = stringResource(MR.strings.migrate).lowercase(),
        onConfirm = {
            scope.launchIO {
                viewModel.migrateManga(replace = true)
                withUIContext { onComplete() }
            }
        },
        dismissLabel = stringResource(MR.strings.action_cancel).lowercase(),
        neutralLabel = stringResource(MR.strings.copy).lowercase(),
        onNeutral = {
            scope.launchIO {
                viewModel.migrateManga(replace = false)
                withUIContext { onComplete() }
            }
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                state.applicableFlags.fastForEach { flag ->
                    val checked = flag in state.selectedFlags
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .metroClickable { viewModel.toggleSelection(flag) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MetroCheckBox(
                            checked = checked,
                            onCheckedChange = null,
                            size = 20.dp,
                        )
                        MetroText(
                            text = stringResource(flag.getLabel()).lowercase(),
                            style = MetroTextStyle.DialogBody,
                            color = MetroTheme.colors.primaryText,
                            maxLines = 1,
                        )
                    }
                }
                MetroBorderButton(
                    text = stringResource(MR.strings.action_show_manga).lowercase(),
                    onClick = {
                        onDismissRequest()
                        onClickTitle()
                    },
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
    )
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class MigrateDialogViewModel(
    private val sourcePreference: SourcePreferences,
    private val coverCache: CoverCache,
    private val downloadManager: DownloadManager,
    private val migrateManga: MigrateMangaUseCase,
) : ViewModel() {

    val state: StateFlow<MigrateDialogViewModel.State>
        field = MutableStateFlow<MigrateDialogViewModel.State>(State())

    fun init(current: Manga, target: Manga) {
        val applicableFlags = buildList {
            MigrationFlag.entries.forEach {
                val applicable = when (it) {
                    MigrationFlag.CHAPTER -> true
                    MigrationFlag.CATEGORY -> true
                    MigrationFlag.CUSTOM_COVER -> current.hasCustomCover(coverCache)
                    MigrationFlag.NOTES -> current.notes.isNotBlank()
                    MigrationFlag.REMOVE_DOWNLOAD -> downloadManager.getDownloadCount(current) > 0
                }
                if (applicable) add(it)
            }
        }
        val selectedFlags = sourcePreference.migrationFlags.get()
        state.update {
            State(
                current = current,
                target = target,
                applicableFlags = applicableFlags,
                selectedFlags = selectedFlags,
            )
        }
    }

    fun toggleSelection(flag: MigrationFlag) {
        state.update {
            val selectedFlags = it.selectedFlags.toMutableSet()
                .apply { if (contains(flag)) remove(flag) else add(flag) }
                .toSet()
            it.copy(selectedFlags = selectedFlags)
        }
    }

    suspend fun migrateManga(replace: Boolean) {
        val currentState = state.value
        val current = currentState.current ?: return
        val target = currentState.target ?: return
        sourcePreference.migrationFlags.set(currentState.selectedFlags)
        state.update { it.copy(isMigrating = true) }
        migrateManga(current, target, replace)
        state.update { it.copy(isMigrating = false, isMigrated = true) }
    }

    data class State(
        val current: Manga? = null,
        val target: Manga? = null,
        val applicableFlags: List<MigrationFlag> = emptyList(),
        val selectedFlags: Set<MigrationFlag> = emptySet(),
        val isMigrating: Boolean = false,
        val isMigrated: Boolean = false,
    )
}
