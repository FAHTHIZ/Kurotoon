package kurotoon.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kurotoon.catalogue.FacetState
import kurotoon.library.LibraryTag
import kurotoon.ui.browse.Pill
import kurotoon.ui.browse.TagPill
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The library's own genres as one-tap chips; stacking chips narrows to series with all of them.
 * Hidden until the library has enough tags to be worth browsing by.
 */
@Composable
fun LibraryTagRow(
    tags: List<LibraryTag>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tags.size < MIN_TAGS && selected.isEmpty()) return
    // Switched-on chips first so they stay in reach while the rest scroll
    val ordered = remember(tags, selected) { tags.sortedByDescending { it.key in selected } }
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (selected.isNotEmpty()) {
            item(key = "clear") {
                Pill(onClick = onClear, height = 30.dp, background = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Text(
                        text = stringResource(MR.strings.kurotoon_browse_clearAll, selected.size),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        items(ordered, key = { it.key }) { tag ->
            TagPill(
                text = "${tag.name} ${tag.count}",
                state = if (tag.key in selected) FacetState.Included else FacetState.Off,
                onClick = { onToggle(tag.key) },
                onLongClick = null,
            )
        }
    }
}

private const val MIN_TAGS = 3
