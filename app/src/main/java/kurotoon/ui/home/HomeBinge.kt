package kurotoon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.manga.components.MangaCover
import kurotoon.ui.KuroColors
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.DoneAll
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** Finished series saved but not started yet. Draws nothing when there are none. */
@Composable
internal fun HomeBinge(items: List<LibraryManga>, onMangaClick: (Long) -> Unit) {
    if (items.isEmpty()) return
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                MaterialSymbols.Rounded.DoneAll,
                contentDescription = null,
                tint = KuroColors.Crimson,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = stringResource(MR.strings.kurotoon_home_binge),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(MR.strings.kurotoon_home_bingeSummary),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { item ->
                Column(modifier = Modifier.width(104.dp)) {
                    MangaCover.Book(
                        data = item.manga.asMangaCover(),
                        shape = RoundedCornerShape(12.dp),
                        contentDescription = item.manga.title,
                        onClick = { onMangaClick(item.id) },
                    )
                    Text(
                        text = item.manga.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        text = pluralStringResource(
                            MR.plurals.kurotoon_history_chapters,
                            item.totalChapters.toInt(),
                            item.totalChapters.toInt(),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
