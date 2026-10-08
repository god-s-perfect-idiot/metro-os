package eu.kanade.tachiyomi.ui.download

import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.databinding.DownloadListBinding
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

object DownloadQueueScreen : Screen() {

    @Composable
    override fun Content() {
        @Suppress("UNUSED_VARIABLE")
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val viewModel = metroViewModel<DownloadQueueViewModel>()
        val downloadList by viewModel.state.collectAsStateWithLifecycle()
        val downloadCount by remember {
            derivedStateOf { downloadList.sumOf { it.subItems.size } }
        }
        val isRunning by viewModel.isDownloaderRunning.collectAsStateWithLifecycle()

        val pageTitle = buildString {
            append(stringResource(MR.strings.label_download_queue).lowercase())
            if (downloadCount > 0) {
                append(" ($downloadCount)")
            }
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
                        pageTitle = pageTitle,
                        appTitle = "metron",
                    )
                    if (downloadList.isEmpty()) {
                        MetroEmptyState(
                            message = stringResource(MR.strings.information_no_downloads),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = MetroAppBarDefaults.BarHeight),
                        )
                    } else {
                        val density = LocalDensity.current
                        val bottomPad = with(density) {
                            (MetroAppBarDefaults.BarHeight + 32.dp).toPx().roundToInt()
                        }
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            factory = { context ->
                                viewModel.controllerBinding =
                                    DownloadListBinding.inflate(LayoutInflater.from(context))
                                viewModel.adapter = DownloadAdapter(viewModel.listener)
                                viewModel.controllerBinding.root.adapter = viewModel.adapter
                                viewModel.adapter?.isHandleDragEnabled = true
                                viewModel.controllerBinding.root.layoutManager =
                                    LinearLayoutManager(context)

                                ViewCompat.setNestedScrollingEnabled(
                                    viewModel.controllerBinding.root,
                                    true,
                                )

                                scope.launchUI {
                                    viewModel.getDownloadStatusFlow()
                                        .collect(viewModel::onStatusChange)
                                }
                                scope.launchUI {
                                    viewModel.getDownloadProgressFlow()
                                        .collect(viewModel::onUpdateDownloadedPages)
                                }

                                viewModel.controllerBinding.root
                            },
                            update = {
                                viewModel.controllerBinding.root.updatePadding(bottom = bottomPad)
                                viewModel.adapter?.updateDataSet(downloadList)
                            },
                        )
                    }
                }

                if (downloadList.isNotEmpty()) {
                    MetroAppBar(
                        icons = listOf(
                            MetroAppBarIcon(
                                type = if (isRunning) {
                                    MetroSystemIconType.Pause
                                } else {
                                    MetroSystemIconType.Play
                                },
                                label = stringResource(
                                    if (isRunning) MR.strings.action_pause else MR.strings.action_resume,
                                ).lowercase(),
                                onClick = {
                                    if (isRunning) {
                                        viewModel.pauseDownloads()
                                    } else {
                                        viewModel.startDownloads()
                                    }
                                },
                            ),
                        ),
                        menuItems = listOf(
                            MetroAppBarMenuItem(
                                text = stringResource(MR.strings.action_newest).lowercase(),
                                onClick = {
                                    viewModel.reorderQueue(
                                        { it.download.chapter.dateUpload },
                                        true,
                                    )
                                },
                            ),
                            MetroAppBarMenuItem(
                                text = stringResource(MR.strings.action_oldest).lowercase(),
                                onClick = {
                                    viewModel.reorderQueue(
                                        { it.download.chapter.dateUpload },
                                        false,
                                    )
                                },
                            ),
                            MetroAppBarMenuItem(
                                text = stringResource(MR.strings.action_cancel_all).lowercase(),
                                onClick = { viewModel.clearQueue() },
                            ),
                        ),
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}
