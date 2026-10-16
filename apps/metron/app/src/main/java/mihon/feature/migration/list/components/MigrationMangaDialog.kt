package mihon.feature.migration.list.components

import androidx.compose.runtime.Composable
import com.metro.ui.MetroMessageDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MigrationMangaDialog(
    onDismissRequest: () -> Unit,
    copy: Boolean,
    totalCount: Int,
    skippedCount: Int,
    onMigrate: () -> Unit,
) {
    MetroMessageDialog(
        title = pluralStringResource(
            resource = if (copy) {
                MR.plurals.migrationListScreen_migrateDialog_copyTitle
            } else {
                MR.plurals.migrationListScreen_migrateDialog_migrateTitle
            },
            count = totalCount,
            totalCount,
        ).lowercase(),
        body = if (skippedCount > 0) {
            pluralStringResource(
                resource = MR.plurals.migrationListScreen_migrateDialog_skipText,
                count = skippedCount,
                skippedCount,
            ).lowercase()
        } else {
            null
        },
        onDismissRequest = onDismissRequest,
        confirmLabel = stringResource(
            resource = if (copy) {
                MR.strings.migrationListScreen_migrateDialog_copyLabel
            } else {
                MR.strings.migrationListScreen_migrateDialog_migrateLabel
            },
        ).lowercase(),
        onConfirm = onMigrate,
        dismissLabel = stringResource(MR.strings.migrationListScreen_migrateDialog_cancelLabel)
            .lowercase(),
    )
}
