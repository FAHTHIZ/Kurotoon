package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.components.GlobalSearchCardRow
import eu.kanade.presentation.browse.components.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.components.GlobalSearchResultItem
import eu.kanade.presentation.browse.components.GlobalSearchToolbar
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchViewModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kurotoon.catalogue.SourceTagIndex
import kurotoon.library.LibraryDuplicates
import kurotoon.ui.browse.RecentSearchRow
import kurotoon.ui.browse.TagBrowseRow
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun GlobalSearchScreen(
    state: SearchViewModel.State,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    recentSearches: List<String> = emptyList(),
    onRecentSearch: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
    libraryTags: List<String> = emptyList(),
    sourceTags: List<SourceTagIndex.Tag> = emptyList(),
    onTag: (String) -> Unit = {},
) {
    val typed = state.searchQuery.orEmpty().trim()
    val tagMatches = remember(sourceTags, typed) {
        SourceTagIndex.matching(sourceTags, typed, if (typed.isEmpty()) IDLE_TAGS else MATCH_TAGS).map { it.name }
    }
    Scaffold(
        topBar = { scrollBehavior ->
            GlobalSearchToolbar(
                searchQuery = state.searchQuery,
                progress = state.progress,
                total = state.total,
                navigateUp = navigateUp,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                hideSourceFilter = false,
                sourceFilter = state.sourceFilter,
                onChangeSearchFilter = onChangeSearchFilter,
                onlyShowHasResults = state.onlyShowHasResults,
                onToggleResults = onToggleResults,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        if (state.items.isEmpty() &&
            (recentSearches.isNotEmpty() || libraryTags.isNotEmpty() || tagMatches.isNotEmpty())
        ) {
            Column(modifier = Modifier.padding(paddingValues).padding(top = 8.dp)) {
                RecentSearchRow(
                    recent = recentSearches,
                    onSearch = onRecentSearch,
                    onClear = onClearRecentSearches,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                )
                if (typed.isEmpty()) {
                    TagBrowseRow(
                        tags = libraryTags,
                        onTag = onTag,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    )
                }
                TagBrowseRow(
                    tags = tagMatches,
                    onTag = onTag,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    title = stringResource(MR.strings.kurotoon_browse_sourceTags, sourceTags.size),
                )
            }
            return@Scaffold
        }
        GlobalSearchContent(
            items = state.filteredItems,
            contentPadding = paddingValues,
            header = {
                if (typed.isNotEmpty()) {
                    TagBrowseRow(
                        tags = tagMatches,
                        onTag = onTag,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        title = stringResource(MR.strings.kurotoon_browse_matchingTags),
                    )
                }
                CombinedResultsRow(
                    items = state.items,
                    getManga = getManga,
                    onClickItem = onClickItem,
                    onLongClickItem = onLongClickItem,
                )
            },
            getManga = getManga,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
        )
    }
}

@Composable
internal fun GlobalSearchContent(
    items: Map<Source, SearchItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    fromSourceId: Long? = null,
    header: (@Composable () -> Unit)? = null,
) {
    LazyColumn(
        contentPadding = contentPadding,
    ) {
        if (header != null) item(key = "tag-header") { header() }
        items.forEach { (source, result) ->
            item(key = source.id) {
                GlobalSearchResultItem(
                    title = fromSourceId?.let {
                        "▶ ${source.name}".takeIf { source.id == fromSourceId }
                    } ?: source.name,
                    subtitle = LocaleHelper.getLocalizedDisplayName(source.lang),
                    onClick = { onClickSource(source) },
                    modifier = Modifier.animateItem(),
                ) {
                    when (result) {
                        SearchItemResult.Loading -> {
                            GlobalSearchLoadingResultItem()
                        }
                        is SearchItemResult.Success -> {
                            GlobalSearchCardRow(
                                titles = result.result,
                                getManga = getManga,
                                onClick = onClickItem,
                                onLongClick = onLongClickItem,
                            )
                        }
                        is SearchItemResult.Error -> {
                            GlobalSearchErrorResultItem(message = result.throwable.message)
                        }
                    }
                }
            }
        }
    }
}

/**
 * The same series found on two or more sources, shown once each with a ×N badge, ahead of the
 * per-source rows. The copy already in the library is preferred, then the first source to answer.
 */
@Composable
private fun CombinedResultsRow(
    items: Map<Source, SearchItemResult>,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    val combined = remember(items) {
        items.values.asSequence()
            .filterIsInstance<SearchItemResult.Success>()
            .flatMap { it.result.asSequence() }
            .groupBy { LibraryDuplicates.keyOf(it.title) }
            .filterKeys { it.isNotEmpty() }
            .values
            .map { group -> group.distinctBy { it.source } }
            .filter { it.size > 1 }
            .sortedByDescending { it.size }
            .take(COMBINED_LIMIT)
            .map { group -> (group.firstOrNull { it.favorite } ?: group.first()) to group.size }
    }
    if (combined.isEmpty()) return
    val counts = remember(combined) { combined.associate { (manga, count) -> manga.id to count } }
    GlobalSearchResultItem(
        title = stringResource(MR.strings.kurotoon_search_combined),
        subtitle = stringResource(MR.strings.kurotoon_search_combinedSummary),
        onClick = null,
    ) {
        GlobalSearchCardRow(
            titles = combined.map { it.first },
            getManga = getManga,
            onClick = onClickItem,
            onLongClick = onLongClickItem,
            sourceCount = { counts[it.id] ?: 0 },
        )
    }
}

private const val COMBINED_LIMIT = 20
private const val IDLE_TAGS = 30
private const val MATCH_TAGS = 16
