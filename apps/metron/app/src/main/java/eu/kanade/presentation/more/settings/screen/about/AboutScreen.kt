package eu.kanade.presentation.more.settings.screen.about

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroListItem
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.RELEASE_URL
import eu.kanade.tachiyomi.util.lang.toDateTimestampString
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.isFossBuildType
import eu.kanade.tachiyomi.util.system.isNightlyBuildType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Instant

object AboutScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val navigator = LocalNavigator.currentOrThrow
        val crashLogUtil = remember { context.appGraph.crashLogUtil }

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
                        pageTitle = stringResource(MR.strings.pref_category_about).lowercase(),
                        appTitle = "metron",
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = MetroAppBarDefaults.BarHeight + 32.dp),
                    ) {
                        item {
                            MetroListItem(
                                title = stringResource(MR.strings.version).lowercase(),
                                subtitle = getVersionName(withBuildDate = true).lowercase(),
                                onClick = {
                                    val deviceInfo = crashLogUtil.getDebugInfo()
                                    context.copyToClipboard("Debug information", deviceInfo)
                                },
                            )
                        }

                        if (!BuildConfig.DEBUG) {
                            item {
                                MetroListItem(
                                    title = stringResource(MR.strings.whats_new).lowercase(),
                                    onClick = { uriHandler.openUri(RELEASE_URL) },
                                )
                            }
                        }

                        item {
                            MetroListItem(
                                title = stringResource(MR.strings.licenses).lowercase(),
                                onClick = { navigator.push(OpenSourceLicensesScreen()) },
                            )
                        }

                        item {
                            MetroListItem(
                                title = stringResource(MR.strings.privacy_policy).lowercase(),
                                onClick = { uriHandler.openUri("https://mihon.app/privacy/") },
                            )
                        }

                        item {
                            MetroListItem(
                                title = "github",
                                onClick = {
                                    uriHandler.openUri(
                                        "https://github.com/god-s-perfect-idiot/metro-os",
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    fun getVersionName(withBuildDate: Boolean): String {
        return when {
            BuildConfig.DEBUG -> {
                "Debug ${BuildConfig.COMMIT_SHA}".let {
                    if (withBuildDate) {
                        "$it (${getFormattedBuildTime()})"
                    } else {
                        it
                    }
                }
            }
            isNightlyBuildType -> {
                "Nightly r${BuildConfig.COMMIT_COUNT}".let {
                    if (withBuildDate) {
                        "$it (${BuildConfig.COMMIT_SHA}, ${getFormattedBuildTime()})"
                    } else {
                        "$it (${BuildConfig.COMMIT_SHA})"
                    }
                }
            }
            else -> {
                val channel = if (isFossBuildType) "FOSS" else "Stable"
                "$channel v${BuildConfig.VERSION_NAME}".let {
                    if (withBuildDate) {
                        "$it (${getFormattedBuildTime()})"
                    } else {
                        it
                    }
                }
            }
        }
    }

    internal fun getFormattedBuildTime(): String {
        return try {
            Instant.parse(BuildConfig.BUILD_TIME)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toDateTimestampString(
                    UiPreferences.dateFormat(
                        Injekt.get<Context>().appGraph.uiPreferences.dateFormat.get(),
                    ),
                )
        } catch (_: Exception) {
            BuildConfig.BUILD_TIME
        }
    }
}
