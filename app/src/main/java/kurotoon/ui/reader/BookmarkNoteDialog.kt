package kurotoon.ui.reader

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kurotoon.manga.BookmarkNotes
import kurotoon.ui.KuroAlertDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroTextButton
import tachiyomi.presentation.core.i18n.stringResource

/** Write or edit the note on a chapter's bookmark. Saving a note also bookmarks the chapter. */
@Composable
fun BookmarkNoteDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    KuroAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.kurotoon_bookmarkNote_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(BookmarkNotes.MAX_LENGTH) },
                placeholder = { Text(stringResource(MR.strings.kurotoon_bookmarkNote_hint)) },
                supportingText = { Text("${text.length}/${BookmarkNotes.MAX_LENGTH}") },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
        },
        confirmButton = {
            KuroTextButton(onClick = { onSave(text) }) { Text(stringResource(MR.strings.action_save)) }
        },
        dismissButton = {
            KuroTextButton(onClick = onDismiss) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}
