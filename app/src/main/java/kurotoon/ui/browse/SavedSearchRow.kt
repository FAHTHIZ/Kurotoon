package kurotoon.ui.browse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kurotoon.catalogue.SavedSearch
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.Bookmark
import kurotoon.icons.materialsymbols.rounded.BookmarkAdd
import kurotoon.ui.KuroAlertDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroFieldShape
import tachiyomi.presentation.core.components.material.KuroSwitch
import tachiyomi.presentation.core.components.material.KuroTextButton
import tachiyomi.presentation.core.components.material.kuroFieldColors
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Saved searches for the current source as one-tap chips, with a "Save" chip in front while
 * something worth keeping is on screen. Long press a saved chip to rename, pin to Home or delete.
 */
@Composable
fun SavedSearchRow(
    saved: List<SavedSearch>,
    selectedId: Long?,
    canSave: Boolean,
    onSave: () -> Unit,
    onOpen: (SavedSearch) -> Unit,
    onEdit: (SavedSearch) -> Unit,
) {
    if (saved.isEmpty() && !canSave) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canSave) {
            item(key = "save") {
                Pill(
                    onClick = onSave,
                    height = 30.dp,
                    background = Color.Transparent,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        MaterialSymbols.Rounded.BookmarkAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(MR.strings.kurotoon_browse_saveSearch),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }
        items(saved, key = { "saved-${it.id}" }) { search ->
            val selected = search.id == selectedId
            Pill(
                onClick = { onOpen(search) },
                onLongClick = { onEdit(search) },
                height = 30.dp,
                background = MaterialTheme.colorScheme.run { if (selected) primary else surfaceContainerHigh },
            ) {
                Icon(
                    MaterialSymbols.Rounded.Bookmark,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = search.name,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Names a new saved search, or renames one. [onDelete] is only offered when editing. */
@Composable
fun SavedSearchDialog(
    initialName: String,
    initialOnHome: Boolean,
    editing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, onHome: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var onHome by rememberSaveable { mutableStateOf(initialOnHome) }
    KuroAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (editing) MR.strings.kurotoon_browse_editSavedSearch else MR.strings.kurotoon_browse_saveSearch,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    shape = KuroFieldShape,
                    colors = kuroFieldColors(),
                    value = name,
                    onValueChange = { name = it.take(48) },
                    singleLine = true,
                    label = { Text(stringResource(MR.strings.name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(MR.strings.kurotoon_browse_showOnHome),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            stringResource(MR.strings.kurotoon_browse_showOnHomeSummary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    KuroSwitch(checked = onHome, onCheckedChange = { onHome = it })
                }
            }
        },
        confirmButton = {
            KuroTextButton(onClick = { onConfirm(name.trim(), onHome) }, enabled = name.isNotBlank()) {
                Text(stringResource(MR.strings.action_save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    KuroTextButton(onClick = onDelete) {
                        Text(stringResource(MR.strings.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                KuroTextButton(onClick = onDismiss) { Text(stringResource(MR.strings.action_cancel)) }
            }
        },
    )
}
