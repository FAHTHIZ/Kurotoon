package kurotoon.ui.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.util.formatChapterNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kurotoon.manga.MergedChapters
import kurotoon.ui.kuroCard
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.FormatListNumbered
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Shown on a series page when the same series is in the library from other sources and those
 * copies have chapters this one lacks. Opens one merged list where every chapter opens from the
 * source that has it. Draws nothing otherwise.
 */
@Composable
fun MergedChaptersCard(
    manga: Manga,
    chapters: List<Chapter>,
    onOpenChapter: (Chapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val ids = remember(chapters) { chapters.map { it.id to it.read } }
    val merged by produceState<List<MergedChapters.Entry>>(emptyList(), manga.id, manga.title, manga.author, ids) {
        value = runCatching {
            withContext(Dispatchers.IO) {
                val graph = context.appGraph
                val library = graph.getLibraryManga.await().map { it.manga }
                val copies = MergedChapters.sameSeries(manga, library)
                if (copies.isEmpty()) return@withContext emptyList()
                val others = copies.map { copy ->
                    MergedChapters.Copy(
                        manga = copy,
                        sourceName = graph.sourceManager.getOrStub(copy.source).name,
                        chapters = graph.getChaptersByMangaId.await(copy.id, applyScanlatorFilter = true),
                    )
                }
                val hereName = graph.sourceManager.getOrStub(manga.source).name
                MergedChapters.merge(MergedChapters.Copy(manga, hereName, chapters), others)
            }
        }.getOrDefault(emptyList())
    }
    val extra = merged.count { !it.here }
    if (extra == 0) return
    val sources = merged.map { it.sourceName }.distinct().size

    var open by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .kuroCard()
            .clickable { open = true }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                MaterialSymbols.Rounded.FormatListNumbered,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(MR.strings.kurotoon_merged_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = pluralStringResource(MR.plurals.kurotoon_merged_summary, extra, extra, sources),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (open) {
        MergedChaptersSheet(
            entries = merged,
            onDismissRequest = { open = false },
            onOpenChapter = {
                open = false
                onOpenChapter(it)
            },
        )
    }
}

@Composable
private fun MergedChaptersSheet(
    entries: List<MergedChapters.Entry>,
    onDismissRequest: () -> Unit,
    onOpenChapter: (Chapter) -> Unit,
) {
    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column {
            Text(
                text = stringResource(MR.strings.kurotoon_merged_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
            )
            Text(
                text = stringResource(MR.strings.kurotoon_merged_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
            )
            LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
                items(entries, key = { it.chapter.id }) { entry ->
                    MergedRow(entry, onClick = { onOpenChapter(entry.chapter) })
                }
            }
        }
    }
}

@Composable
private fun MergedRow(entry: MergedChapters.Entry, onClick: () -> Unit) {
    val dim = if (entry.read) 0.5f else 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(MR.strings.kurotoon_merged_chapter, formatChapterNumber(entry.number)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = dim),
            )
            if (entry.chapter.name.isNotBlank()) {
                Text(
                    text = entry.chapter.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dim),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = entry.sourceName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (entry.here) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .background(
                    if (entry.here) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    },
                    RoundedCornerShape(50),
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
