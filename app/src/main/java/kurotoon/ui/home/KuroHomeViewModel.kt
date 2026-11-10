package kurotoon.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kurotoon.catalogue.SavedSearch
import kurotoon.catalogue.SavedSearches
import kurotoon.prefs.KuroPreferences
import logcat.LogPriority
import mihon.domain.manga.model.toDomainManga
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.LocalSource

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class KuroHomeViewModel(
    getHistory: GetHistory,
    getEnabledSources: GetEnabledSources,
    private val getNextChapters: GetNextChapters,
    private val sourceManager: SourceManager,
    private val networkToLocalManga: NetworkToLocalManga,
    kuroPreferences: KuroPreferences,
) : ViewModel() {

    private val rows = MutableStateFlow<List<SourceRow>>(emptyList())
    private val savedRows = MutableStateFlow<List<SourceRow>>(emptyList())
    private val homeSearches = kuroPreferences.savedSearches.changes()
        .catch { logcat(LogPriority.ERROR, it) }
        .map { all -> all.filter { it.onHome }.take(HOME_SAVED_SEARCHES) }
        .distinctUntilChanged()
    private val sourceIds = getEnabledSources.subscribe()
        .catch { logcat(LogPriority.ERROR, it) }
        .map { sources -> sources.filterNot { it.id == LocalSource.ID }.map { it.id }.take(HOME_SOURCES) }
        .distinctUntilChanged()

    private val openChapterEvents = Channel<Chapter?>(Channel.UNLIMITED)

    /** The chapter to open after tapping "continue"; null when the series has nothing left. */
    val openChapter: Flow<Chapter?> = openChapterEvents.receiveAsFlow()

    val state: StateFlow<State> = combine(
        getHistory.subscribe("").catch { logcat(LogPriority.ERROR, it) },
        sourceIds,
        rows,
        savedRows,
    ) { history, ids, rows, savedRows ->
        State(
            isLoading = false,
            continueReading = history.distinctBy { it.mangaId }.take(CONTINUE_READING_LIMIT),
            hasSources = ids.isNotEmpty(),
            rows = rows,
            savedRows = savedRows,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())

    init {
        viewModelScope.launch {
            sourceIds.collectLatest { ids -> loadRows(ids) }
        }
        viewModelScope.launch {
            homeSearches.collectLatest { searches -> loadSavedRows(searches) }
        }
    }

    /** Saved searches pinned to Home, each as its own row (like a feed). */
    private suspend fun loadSavedRows(searches: List<SavedSearch>) = coroutineScope {
        savedRows.value = searches.mapNotNull { search ->
            sourceManager.get(search.sourceId)?.let { source ->
                SourceRow(source.id, search.name, items = null, savedSearch = search, sourceName = source.name)
            }
        }
        savedRows.value.forEach { row -> launch { loadRow(row) } }
    }

    /** Fetches each source's popular list, like the web app's home rows. */
    private suspend fun loadRows(ids: List<Long>) = coroutineScope {
        rows.value = ids.mapNotNull { id ->
            sourceManager.get(id)?.let { SourceRow(it.id, it.name, items = null) }
        }
        rows.value.forEach { row ->
            launch { loadRow(row) }
        }
    }

    fun retry(row: SourceRow) {
        viewModelScope.launch { loadRow(row) }
    }

    private suspend fun loadRow(row: SourceRow) {
        val source = sourceManager.get(row.sourceId) ?: return
        updateRow(row.key) { it.copy(items = null, failed = false) }
        val search = row.savedSearch
        val result = runCatching {
            val page = withContext(Dispatchers.IO) {
                if (search != null && source is CatalogueSource) {
                    val filters = SavedSearches.restore(source.getFilterList(), search.filters)
                    source.getSearchManga(1, search.query.orEmpty(), filters)
                } else {
                    source.getPopularManga(1)
                }
            }
            page.mangas
                .map { it.toDomainManga(source.id) }
                .distinctBy { it.url }
                .let { networkToLocalManga(it) }
                .take(ROW_LIMIT)
        }
        result.exceptionOrNull()?.let { logcat(LogPriority.WARN, it) { "Home row failed for ${source.name}" } }
        updateRow(row.key) { it.copy(items = result.getOrNull(), failed = result.isFailure) }
    }

    private fun updateRow(key: String, block: (SourceRow) -> SourceRow) {
        val target = if (key.startsWith(SAVED_PREFIX)) savedRows else rows
        target.update { list -> list.map { if (it.key == key) block(it) else it } }
    }

    fun resume(item: HistoryWithRelations) {
        viewModelScope.launch {
            openChapterEvents.send(
                getNextChapters.await(item.mangaId, item.chapterId, onlyUnread = false).firstOrNull(),
            )
        }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val continueReading: List<HistoryWithRelations> = emptyList(),
        val hasSources: Boolean = true,
        val rows: List<SourceRow> = emptyList(),
        /** Rows for saved searches the user pinned to Home. */
        val savedRows: List<SourceRow> = emptyList(),
    ) {
        /** Top titles for the hero carousel, taking turns between sources so each gets a slot. */
        val heroes: List<Pair<SourceRow, Manga>>
            get() {
                val picks = rows.map { row -> row.items.orEmpty().filter { !it.thumbnailUrl.isNullOrBlank() } }
                return buildList {
                    var depth = 0
                    while (size < HERO_COUNT && picks.any { it.size > depth }) {
                        rows.forEachIndexed { i, row ->
                            picks[i].getOrNull(depth)?.let { if (size < HERO_COUNT) add(row to it) }
                        }
                        depth++
                    }
                }
            }
    }

    @Immutable
    data class SourceRow(
        val sourceId: Long,
        val name: String,
        /** Null while loading. */
        val items: List<Manga>?,
        val failed: Boolean = false,
        /** Set when the row shows a saved search instead of the source's popular list. */
        val savedSearch: SavedSearch? = null,
        val sourceName: String = name,
    ) {
        val key: String get() = savedSearch?.let { "$SAVED_PREFIX${it.id}" } ?: "source-$sourceId"
    }

    private companion object {
        const val CONTINUE_READING_LIMIT = 15
        const val HOME_SOURCES = 4
        const val ROW_LIMIT = 18
        const val HOME_SAVED_SEARCHES = 6
    }
}

private const val SAVED_PREFIX = "saved-"

private const val HERO_COUNT = 6
