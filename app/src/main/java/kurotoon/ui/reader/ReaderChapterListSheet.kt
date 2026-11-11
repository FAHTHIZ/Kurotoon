package kurotoon.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.Bookmark
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Every chapter of the series, in reading order, so the reader can jump straight to any of them
 * without going back to the series page. The current chapter is highlighted and scrolled into view.
 */
@Composable
fun ReaderChapterListSheet(
    chapters: List<Chapter>,
    currentChapterId: Long?,
    onDismissRequest: () -> Unit,
    onChapterClick: (Chapter) -> Unit,
) {
    val currentIndex = chapters.indexOfFirst { it.id == currentChapterId }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex - 3).coerceAtLeast(0))

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column {
            Text(
                text = stringResource(MR.strings.kurotoon_reader_chapterList),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.heightIn(max = 480.dp),
            ) {
                itemsIndexed(chapters, key = { _, chapter -> chapter.id }) { _, chapter ->
                    val current = chapter.id == currentChapterId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (current) {
                                    Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable {
                                onDismissRequest()
                                if (!current) onChapterClick(chapter)
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = chapter.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (current) FontWeight.SemiBold else null,
                            color = when {
                                current -> MaterialTheme.colorScheme.onPrimaryContainer
                                chapter.read -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (chapter.bookmark) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = MaterialSymbols.RoundedFilled.Bookmark,
                                contentDescription = stringResource(MR.strings.action_bookmark),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
