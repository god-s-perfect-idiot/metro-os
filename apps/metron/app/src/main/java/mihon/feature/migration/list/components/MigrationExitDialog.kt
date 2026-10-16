package mihon.feature.migration.list.components

import androidx.compose.runtime.Composable
import com.metro.ui.MetroMessageDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MigrationExitDialog(
    onDismissRequest: () -> Unit,
    exitMigration: () -> Unit,
) {
    MetroMessageDialog(
        title = stringResource(MR.strings.migrationListScreen_exitDialogTitle).lowercase(),
        onDismissRequest = onDismissRequest,
        confirmLabel = stringResource(MR.strings.migrationListScreen_exitDialog_stopLabel).lowercase(),
        onConfirm = exitMigration,
        dismissLabel = stringResource(MR.strings.migrationListScreen_exitDialog_cancelLabel).lowercase(),
    )
}
