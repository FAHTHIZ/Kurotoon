package mihon.feature.migration.list.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.DialogProperties
import kurotoon.ui.KuroAlertDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroTextButton
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MigrationProgressDialog(
    progress: Float,
    exitMigration: () -> Unit,
) {
    KuroAlertDialog(
        onDismissRequest = {},
        confirmButton = {
            KuroTextButton(onClick = exitMigration) {
                Text(text = stringResource(MR.strings.migrationListScreen_progressDialog_cancelLabel))
            }
        },
        text = {
            if (!progress.isNaN()) {
                val progressAnimated by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    label = "migration_progress",
                )
                tachiyomi.presentation.core.components.KuroProgressBar(
                    progress = { progressAnimated },
                )
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    )
}
