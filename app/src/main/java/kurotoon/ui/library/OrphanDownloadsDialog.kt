package kurotoon.ui.library

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kurotoon.library.OrphanDownloads
import kurotoon.ui.KuroAlertDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** Shows exactly which series folders will go before anything is deleted. */
@Composable
fun OrphanDownloadsDialog(
    orphans: List<OrphanDownloads.Orphan>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    KuroAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pluralStringResource(MR.plurals.kurotoon_orphans_found, orphans.size, orphans.size)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(orphans, key = { it.source + "/" + it.title }) {
                    Text(
                        text = "${it.title} · ${it.source}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(MR.strings.kurotoon_orphans_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}
