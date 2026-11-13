package kurotoon.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.library.components.MangaComfortableGridItem
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.CollectionsBookmark
import kurotoon.icons.materialsymbols.rounded.Error
import kurotoon.icons.materialsymbols.rounded.KeyboardArrowRight
import kurotoon.icons.materialsymbols.rounded.TravelExplore
import kurotoon.search.UnifiedSearch
import kurotoon.ui.kuroBounceClickable
import kurotoon.ui.kuroCard
import kurotoon.ui.kuroEntrance
import kurotoon.ui.kuroShimmer
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Universal search as one grid: every source's answer folded together, the same series once with a
 * ×N badge, best match first. Long-press a cover to pick which source's copy to open.
 */
@Composable
fun UnifiedSearchResults(
    items: Map<Source, SearchItemResult>,
    query: String,
    library: List<Manga>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickItem: (Manga) -> Unit,
    onShowBySource: () -> Unit,
    header: @Composable () -> Unit = {},
) {
    val hits = remember(items, query, library) {
        UnifiedSearch.merge(
            results = items.values.filterIsInstance<SearchItemResult.Success>().map { it.result },
            query = query,
            library = library,
        )
    }
    val sources = remember(items) { items.keys.associateBy { it.id } }
    val answered = items.values.count { it !is SearchItemResult.Loading }
    val waiting = items.size - answered
    val failed = items.values.count { it is SearchItemResult.Error }
    var picking by remember { mutableStateOf<UnifiedSearch.Hit?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(108.dp),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
        item(key = "summary", span = { GridItemSpan(maxLineSpan) }) {
            SearchSummary(
                found = hits.size,
                answered = answered,
                total = items.size,
                waiting = waiting,
                failed = failed,
                onShowBySource = onShowBySource,
            )
        }
        itemsIndexed(hits, key = { _, hit -> "hit-${hit.key}" }) { index, hit ->
            val manga by getManga(hit.primary)
            Box(modifier = Modifier.animateItem().kuroEntrance(index)) {
                MangaComfortableGridItem(
                    title = manga.title,
                    titleMaxLines = 2,
                    coverData = manga.asMangaCover(),
                    coverBadgeStart = {
                        if (manga.favorite) Badge(imageVector = MaterialSymbols.Rounded.CollectionsBookmark)
                    },
                    coverBadgeEnd = {
                        if (hit.sourceCount > 1) Badge(text = "×${hit.sourceCount}")
                    },
                    onClick = { onClickItem(manga) },
                    onLongClick = { if (hit.sourceCount > 1) picking = hit else onClickItem(manga) },
                )
            }
        }
        if (hits.isEmpty() && waiting > 0) {
            items(count = PLACEHOLDERS, key = { "placeholder-$it" }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .kuroShimmer(RoundedCornerShape(12.dp), MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }
        }
        if (hits.isEmpty() && waiting == 0 && items.isNotEmpty()) {
            item(key = "nothing", span = { GridItemSpan(maxLineSpan) }) {
                NothingFound()
            }
        }
    }

    picking?.let { hit ->
        CopyPickerSheet(
            hit = hit,
            sourceName = { id -> sources[id]?.let { "${it.name} · ${LocaleHelper.getLocalizedDisplayName(it.lang)}" } },
            onPick = {
                picking = null
                onClickItem(it)
            },
            onDismiss = { picking = null },
        )
    }
}

@Composable
private fun SearchSummary(
    found: Int,
    answered: Int,
    total: Int,
    waiting: Int,
    failed: Int,
    onShowBySource: () -> Unit,
) {
    if (total == 0) return
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(MR.strings.kurotoon_search_summary, found, answered, total),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (waiting > 0) {
                Text(
                    text = stringResource(MR.strings.kurotoon_search_stillAsking, waiting),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (failed > 0 && waiting == 0) {
            Row(
                modifier = Modifier
                    .kuroBounceClickable(onClick = onShowBySource)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    MaterialSymbols.Rounded.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(MR.strings.kurotoon_search_failed, failed),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = stringResource(MR.strings.kurotoon_search_seeBySource),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun NothingFound() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .kuroCard(RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                MaterialSymbols.Rounded.TravelExplore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(MR.strings.kurotoon_search_nothing),
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
private fun CopyPickerSheet(
    hit: UnifiedSearch.Hit,
    sourceName: (Long) -> String?,
    onPick: (Manga) -> Unit,
    onDismiss: () -> Unit,
) {
    AdaptiveSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = hit.primary.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
            )
            Text(
                text = stringResource(MR.strings.kurotoon_search_foundOn, hit.sourceCount) + " · " +
                    stringResource(MR.strings.kurotoon_search_pickCopy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
            )
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(hit.copies, key = { "${it.source}:${it.url}" }) { copy ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .kuroBounceClickable(onClick = { onPick(copy) })
                            .kuroCard(RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sourceName(copy.source) ?: copy.source.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (copy.favorite) {
                                Text(
                                    text = stringResource(MR.strings.kurotoon_search_inLibrary),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Icon(
                            MaterialSymbols.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private const val PLACEHOLDERS = 9
