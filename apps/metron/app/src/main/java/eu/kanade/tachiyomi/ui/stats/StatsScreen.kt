package eu.kanade.tachiyomi.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.more.stats.StatsScreenContent
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

class StatsScreen : Screen() {

    @Composable
    override fun Content() {
        @Suppress("UNUSED_VARIABLE")
        val navigator = LocalNavigator.currentOrThrow

        val viewModel = metroViewModel<StatsViewModel>()
        val state by viewModel.state.collectAsState()

        MetroSystemTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .metroNavBarPadding()
                    .background(MetroTheme.colors.background),
            ) {
                MetroSettingsHeader(
                    pageTitle = stringResource(MR.strings.label_stats).lowercase(),
                    appTitle = "metron",
                )
                when (val current = state) {
                    is StatsScreenState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            MetroLoadingDots(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    is StatsScreenState.Success -> {
                        StatsScreenContent(
                            state = current,
                            paddingValues = PaddingValues(bottom = MetroAppBarDefaults.BarHeight + 32.dp),
                        )
                    }
                }
            }
        }
    }
}
