package kurotoon.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.BookmarkAdd
import kurotoon.icons.materialsymbols.rounded.DoneAll
import kurotoon.icons.materialsymbols.rounded.History
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** Finished series saved but not started yet. Draws nothing when there are none. */
@Composable
internal fun HomeBinge(items: List<LibraryManga>, onMangaClick: (Long) -> Unit) {
    HomeLibraryShelf(
        title = stringResource(MR.strings.kurotoon_home_binge),
        summary = stringResource(MR.strings.kurotoon_home_bingeSummary),
        icon = MaterialSymbols.Rounded.DoneAll,
        items = items,
        onMangaClick = onMangaClick,
    ) { item ->
        pluralStringResource(
            MR.plurals.kurotoon_history_chapters,
            item.totalChapters.toInt(),
            item.totalChapters.toInt(),
        )
    }
}

/** Started series nobody has opened for a few weeks. Draws nothing when there are none. */
@Composable
internal fun HomeStalled(items: List<LibraryManga>, onMangaClick: (Long) -> Unit) {
    HomeLibraryShelf(
        title = stringResource(MR.strings.kurotoon_home_stalled),
        summary = stringResource(MR.strings.kurotoon_home_stalledSummary),
        icon = MaterialSymbols.Rounded.History,
        items = items,
        onMangaClick = onMangaClick,
    ) { item ->
        stringResource(MR.strings.kurotoon_home_left, item.unreadCount.toInt())
    }
}

/** Series saved in the last month, newest first. Draws nothing when there are none. */
@Composable
internal fun HomeRecentlyAdded(items: List<LibraryManga>, onMangaClick: (Long) -> Unit) {
    val now = remember { System.currentTimeMillis() }
    HomeLibraryShelf(
        title = stringResource(MR.strings.kurotoon_home_recentlyAdded),
        summary = stringResource(MR.strings.kurotoon_home_recentlyAddedSummary),
        icon = MaterialSymbols.Rounded.BookmarkAdd,
        items = items,
        onMangaClick = onMangaClick,
    ) { item ->
        val added = item.manga.favoriteAt?.toEpochMilliseconds() ?: now
        DateUtils.getRelativeTimeSpanString(added, now, DateUtils.DAY_IN_MILLIS).toString()
    }
}

/** A titled row of library covers with a one-line [caption] under each. */
@Composable
private fun HomeLibraryShelf(
    title: String,
    summary: String,
    icon: ImageVector,
    items: List<LibraryManga>,
    onMangaClick: (Long) -> Unit,
    caption: @Composable (LibraryManga) -> String,
) {
    if (items.isEmpty()) return
    Column {
        HomeShelfHeader(title = title, icon = icon, subtitle = summary)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { item ->
                Column(modifier = Modifier.width(HomeCoverWidth)) {
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
                        text = caption(item),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
