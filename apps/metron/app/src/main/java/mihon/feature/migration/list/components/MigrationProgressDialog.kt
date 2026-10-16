package mihon.feature.migration.list.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MigrationProgressDialog(
    progress: Float,
    exitMigration: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        MetroSystemTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(MetroTheme.colors.background, RectangleShape)
                        .padding(24.dp),
                ) {
                    MetroText(
                        text = stringResource(MR.strings.migrationListScreenTitle).lowercase(),
                        style = MetroTextStyle.SectionHeader,
                        color = MetroTheme.colors.primaryText,
                    )
                    if (!progress.isNaN()) {
                        val progressAnimated by animateFloatAsState(
                            targetValue = progress.coerceIn(0f, 1f),
                            animationSpec = tween(
                                durationMillis = MetroTransitions.PageTransitionMs,
                                easing = MetroTransitions.PageEasing,
                            ),
                            label = "migration_progress",
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .height(3.dp)
                                .background(MetroTheme.colors.secondarySurface, RectangleShape),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressAnimated)
                                    .height(3.dp)
                                    .background(MetroTheme.colors.accent, RectangleShape),
                            )
                        }
                    }
                    MetroBorderButton(
                        text = stringResource(
                            MR.strings.migrationListScreen_progressDialog_cancelLabel,
                        ).lowercase(),
                        onClick = exitMigration,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }
            }
        }
    }
}
