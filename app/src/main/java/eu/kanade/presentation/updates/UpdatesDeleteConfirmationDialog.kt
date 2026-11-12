package eu.kanade.presentation.updates

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kurotoon.ui.KuroAlertDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroTextButton
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun UpdatesDeleteConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    KuroAlertDialog(
        text = {
            Text(text = stringResource(MR.strings.confirm_delete_chapters))
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            KuroTextButton(onClick = {
                onConfirm()
                onDismissRequest()
            }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            KuroTextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}
