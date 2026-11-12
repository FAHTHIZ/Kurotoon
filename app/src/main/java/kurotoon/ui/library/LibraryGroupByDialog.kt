package kurotoon.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kurotoon.library.LibraryGroupBy
import kurotoon.ui.KuroAlertDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.RadioItem
import tachiyomi.presentation.core.i18n.stringResource

/** Picks how the library splits into tabs. */
@Composable
fun LibraryGroupByDialog(
    selected: LibraryGroupBy,
    onSelect: (LibraryGroupBy) -> Unit,
    onDismissRequest: () -> Unit,
) {
    KuroAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.kurotoon_library_groupBy)) },
        text = {
            Column {
                LibraryGroupBy.entries.forEach { mode ->
                    RadioItem(
                        label = stringResource(
                            when (mode) {
                                LibraryGroupBy.CATEGORY -> MR.strings.kurotoon_library_groupBy_category
                                LibraryGroupBy.SOURCE -> MR.strings.kurotoon_library_groupBy_source
                                LibraryGroupBy.STATUS -> MR.strings.kurotoon_library_groupBy_status
                                LibraryGroupBy.NONE -> MR.strings.kurotoon_library_groupBy_none
                            },
                        ),
                        selected = mode == selected,
                        onClick = { onSelect(mode) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}
