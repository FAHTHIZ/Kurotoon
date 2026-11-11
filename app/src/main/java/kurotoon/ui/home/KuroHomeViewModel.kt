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
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.util.removeCovers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kurotoon.catalogue.CatalogFacets
import kurotoon.catalogue.HiddenTags
import kurotoon.catalogue.SavedSearch
import kurotoon.catalogue.SavedSearches
import kurotoon.catalogue.SearchFeed
import kurotoon.library.BingeList
import kurotoon.library.LibraryTags
import kurotoon.library.RecentlyAddedList
import kurotoon.library.StalledList
import kurotoon.prefs.KuroPreferences
import kurotoon.schedule.ScheduleLoader
import kurotoon.schedule.WeeklySchedule
import logcat.LogPriority
import mihon.domain.manga.model.toDomainManga
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.chapter.interactor.SetMangaDefaultChapterFlags
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.updates.interactor.GetUpdates
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.source.local.LocalSource
import kotlin.time.Clock

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class KuroHomeViewModel(
    getHistory: GetHistory,
    getEnabledSources: GetEnabledSources,
    private val getNextChapters: GetNextChapters,
    private val sourceManager: SourceManager,
    private val networkToLocalManga: NetworkToLocalManga,
    private val kuroPreferences: KuroPreferences,
    getUpdates: GetUpdates,
    private val getLibraryManga: GetLibraryManga,
    scheduleLoader: ScheduleLoader,
    private val updateManga: eu.kanade.domain.manga.interactor.UpdateManga,
    private val libraryPreferences: LibraryPreferences,
    private val getCategories: GetCategories,
    private val setMangaCategories: SetMangaCategories,
    private val setMangaDefaultChapterFlags: SetMangaDefaultChapterFlags,
    private val coverCache: CoverCache,
    private val getChapter: tachiyomi.domain.chapter.interactor.GetChapter,
) : ViewModel() {

    /** Unread chapters fetched lately, one entry per series, newest first. */
    private val newChapters = getUpdates.subscribe(
        Clock.System.now().minus(NEW_CHAPTER_DAYS, DateTimeUnit.DAY, TimeZone.currentSystemDefault()),
        unread = true,
        started = null,
        bookmarked = null,
        hideExcludedScanlators = true,
        includedCategories = emptyList(),
        excludedCategories = emptyList(),
    )
        .catch { logcat(LogPriority.ERROR, it) }
        .map { updates ->
            updates.groupBy { it.mangaId }.values
                .map { chapters -> NewChapters(chapters.first(), chapters.size) }
                .take(NEW_CHAPTERS_LIMIT)
        }
        .distinctUntilChanged()

    /** Finished series in the library that haven't been started. */
    private val shelves = getLibraryManga.subscribe()
        .catch { logcat(LogPriority.ERROR, it) }
        .map { library ->
            val now = Clock.System.now()
            Triple(BingeList.of(library), StalledList.of(library, now), RecentlyAddedList.of(library, now))
        }
        .distinctUntilChanged()

    /** This week's release schedule, for the "Out today" row. */
    private val schedule = scheduleLoader.subscribe()

    private val rows = MutableStateFlow<List<SourceRow>>(emptyList())
    private val savedRows = MutableStateFlow<List<SourceRow>>(emptyList())
    private val pickRows = MutableStateFlow<List<SourceRow>>(emptyList())
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
        combine(savedRows, pickRows) { saved, picks -> saved to picks },
        combine(newChapters, shelves, schedule) { new, shelves, schedule -> Triple(new, shelves, schedule) },
    ) { history, ids, rows, (savedRows, pickRows), (newChapters, shelves, schedule) ->
        val continueReading = history.distinctBy { it.mangaId }.take(CONTINUE_READING_LIMIT)
        State(
            isLoading = false,
            continueReading = continueReading,
            continuePages = continueReading.mapNotNull { item ->
                val chapter = runCatching { getChapter.await(item.chapterId) }.getOrNull() ?: return@mapNotNull null
                if (chapter.read || chapter.lastPageRead <= 0) return@mapNotNull null
                item.chapterId to (chapter.lastPageRead + 1).toInt()
            }.toMap(),
            hasSources = ids.isNotEmpty(),
            rows = rows,
            savedRows = savedRows,
            pickRows = pickRows,
            newChapters = newChapters,
            binge = shelves.first,
            stalled = shelves.second,
            recentlyAdded = shelves.third,
            schedule = schedule,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())

    /**
     * The fetch in flight for each row, so a newer one (toggle, retry, refresh) replaces an older one.
     * Declared before init: the first loads start from init, and a later field would still be null then.
     */
    private val rowJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    init {
        viewModelScope.launch {
            sourceIds.collectLatest { ids ->
                coroutineScope {
                    launch { loadRows(ids) }
                    launch { loadPickRows(ids) }
                }
            }
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
        savedRows.value.forEach(::launchRow)
    }

    /**
     * "Because you read X" rows: the library's most common tags, each searched on the first
     * Home source that has that tag as a filter, so picks follow the user's own taste.
     */
    private suspend fun loadPickRows(ids: List<Long>) = coroutineScope {
        // Picks cost one search per row; skip them when the reader switched the section off
        if (HomeSection.PICKS.key in kuroPreferences.homeHiddenSections.get()) {
            pickRows.value = emptyList()
            return@coroutineScope
        }
        val library = runCatching { withContext(Dispatchers.IO) { getLibraryManga.await() } }
            .onFailure { logcat(LogPriority.WARN, it) }
            .getOrNull()
            .orEmpty()
        val hidden = HiddenTags.parse(kuroPreferences.hiddenTags.get())
        val tags = LibraryTags.top(library.map { it.manga.genre }, PICK_TAG_CANDIDATES)
            .filter { it.count >= PICK_MIN_COUNT && !HiddenTags.hides(listOf(it.name), hidden) }
        val sources = ids.mapNotNull { sourceManager.get(it) as? CatalogueSource }
        val picks = mutableListOf<SourceRow>()
        for (tag in tags) {
            if (picks.size >= PICK_ROWS) break
            val source = sources.firstOrNull { source ->
                runCatching { CatalogFacets.includeTag(source.getFilterList(), tag.name) }.getOrDefault(false)
            } ?: continue
            picks += SourceRow(source.id, tag.name, items = null, sourceName = source.name, pickTag = tag.name)
        }
        pickRows.value = picks
        picks.forEach(::launchRow)
    }

    /** Fetches each source's popular list, like the web app's home rows. */
    private suspend fun loadRows(ids: List<Long>) = coroutineScope {
        val latest = kuroPreferences.homeLatestSources.get()
        rows.value = ids.mapNotNull { id ->
            sourceManager.get(id)?.let {
                SourceRow(
                    it.id,
                    it.name,
                    items = null,
                    supportsLatest = it.supportsLatest,
                    latest = it.supportsLatest && it.id.toString() in latest,
                )
            }
        }
        rows.value.forEach(::launchRow)
    }

    /** Switches a source's Home row between popular titles and latest updates, and remembers it. */
    fun toggleLatest(row: SourceRow) {
        if (row.savedSearch != null || !row.supportsLatest) return
        val updated = row.copy(latest = !row.latest)
        val key = row.sourceId.toString()
        kuroPreferences.homeLatestSources.getAndSet { if (updated.latest) it + key else it - key }
        updateRow(row.key) { it.copy(latest = updated.latest) }
        launchRow(updated)
    }

    /** Pull-to-refresh: reloads every row, and rebuilds picks so new library tags show up. */
    fun refresh() {
        viewModelScope.launch {
            launch { loadPickRows(rows.value.map { it.sourceId }) }
            (savedRows.value + rows.value).forEach(::launchRow)
        }
    }

    fun retry(row: SourceRow) = launchRow(row)

    /**
     * Loads [row], cancelling any earlier load of the same row first. Without this a slow Popular
     * fetch could land after the Latest one the reader just switched to, and overwrite it.
     */
    private fun launchRow(row: SourceRow) {
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) { loadRow(row) }
        rowJobs.put(row.key, job)?.cancel()
        job.invokeOnCompletion { rowJobs.remove(row.key, job) }
        job.start()
    }

    private suspend fun loadRow(row: SourceRow) {
        val source = sourceManager.get(row.sourceId) ?: return
        updateRow(row.key) { it.copy(items = null, failed = false) }
        val search = row.savedSearch
        val pickTag = row.pickTag
        val hidden = HiddenTags.parse(kuroPreferences.hiddenTags.get())
        val result = runCatching {
            val page = withContext(Dispatchers.IO) {
                if (pickTag != null && source is CatalogueSource) {
                    val filters = source.getFilterList()
                    CatalogFacets.includeTag(filters, pickTag)
                    HiddenTags.excludeIn(filters, hidden)
                    source.getSearchManga(1, "", filters)
                } else if (search != null && source is CatalogueSource) {
                    val filters = SavedSearches.restore(source.getFilterList(), search.filters)
                    HiddenTags.excludeIn(filters, hidden)
                    source.getSearchManga(1, search.query.orEmpty(), filters)
                } else if (row.latest) {
                    source.getLatestUpdates(1)
                } else {
                    source.getPopularManga(1)
                }
            }
            page.mangas
                .map { it.toDomainManga(source.id) }
                .distinctBy { it.url }
                .let { networkToLocalManga(it) }
                .filterNot { HiddenTags.hides(it.genre, hidden) }
                // Picks are for discovery, so leave out what's already in the library
                .filterNot { pickTag != null && it.favorite }
                .take(ROW_LIMIT)
        }
        result.exceptionOrNull()?.let { logcat(LogPriority.WARN, it) { "Home row failed for ${source.name}" } }
        val fresh = search?.let { saved -> result.getOrNull()?.let { markSeen(saved, it) } }.orEmpty()
        updateRow(row.key) { it.copy(items = result.getOrNull(), failed = result.isFailure, freshUrls = fresh) }
    }

    /** Remembers what a saved search showed and returns the urls it didn't show last time. */
    private fun markSeen(search: SavedSearch, items: List<Manga>): Set<String> {
        val pref = kuroPreferences.savedSearchSeen(search.id)
        val seen = SearchFeed.decode(pref.get())
        val urls = items.map { it.url }
        pref.set(SearchFeed.encode(SearchFeed.remember(seen, urls)))
        return SearchFeed.fresh(seen, urls)
    }

    private val libraryEvents = Channel<Pair<String, Boolean>>(Channel.UNLIMITED)

    /** Title and new library state after a long-press toggled it, for a snackbar. */
    val libraryToggled: Flow<Pair<String, Boolean>> = libraryEvents.receiveAsFlow()

    /** Long-press on a cover: save it to the library, or take it out if it's already there. */
    fun toggleLibrary(manga: Manga) {
        viewModelScope.launch(Dispatchers.IO) {
            val favorite = !manga.favorite
            if (favorite) {
                // Same as adding from a source: the reader's chapter defaults and default category.
                // "Always ask" has no dialog here, so the series lands uncategorised.
                setMangaDefaultChapterFlags.await(manga)
                val defaultId = kuroPreferences.defaultCategoryFor(
                    manga.source,
                    libraryPreferences.defaultCategory.get().toLong(),
                )
                val category = getCategories.await().find { it.id == defaultId && it.id != 0L }
                if (category != null) setMangaCategories.await(manga.id, listOf(category.id))
            } else {
                manga.removeCovers(coverCache)
            }
            if (!updateManga.awaitUpdateFavorite(manga.id, favorite)) return@launch
            val swap: (SourceRow) -> SourceRow = { row ->
                row.copy(
                    items = row.items?.map {
                        if (it.id ==
                            manga.id
                        ) {
                            it.copy(favoriteAt = if (favorite) kotlin.time.Clock.System.now() else null)
                        } else {
                            it
                        }
                    },
                )
            }
            listOf(rows, savedRows, pickRows).forEach { flow -> flow.update { list -> list.map(swap) } }
            libraryEvents.send(manga.title to favorite)
        }
    }

    private fun updateRow(key: String, block: (SourceRow) -> SourceRow) {
        val target = when {
            key.startsWith(SAVED_PREFIX) -> savedRows
            key.startsWith(PICK_PREFIX) -> pickRows
            else -> rows
        }
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
        /** The page each unfinished Continue reading chapter was left on, by chapter id. */
        val continuePages: Map<Long, Int> = emptyMap(),
        val hasSources: Boolean = true,
        val rows: List<SourceRow> = emptyList(),
        /** Rows for saved searches the user pinned to Home. */
        val savedRows: List<SourceRow> = emptyList(),
        /** "Because you read X" rows built from the library's favourite tags. */
        val pickRows: List<SourceRow> = emptyList(),
        val newChapters: List<NewChapters> = emptyList(),
        val binge: List<LibraryManga> = emptyList(),
        /** Started series left alone for a few weeks. */
        val stalled: List<LibraryManga> = emptyList(),
        val recentlyAdded: List<LibraryManga> = emptyList(),
        val schedule: WeeklySchedule = WeeklySchedule.EMPTY,
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

    /** A library series with [count] unread chapters fetched lately; [latest] is the newest. */
    @Immutable
    data class NewChapters(val latest: UpdatesWithRelations, val count: Int)

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
        val supportsLatest: Boolean = false,
        /** Shows latest updates instead of popular titles. */
        val latest: Boolean = false,
        /** Set when the row shows titles with a tag the user reads a lot. */
        val pickTag: String? = null,
        /** Urls of saved-search results that weren't there last time. */
        val freshUrls: Set<String> = emptySet(),
    ) {
        val key: String
            get() = savedSearch?.let { "$SAVED_PREFIX${it.id}" }
                ?: pickTag?.let { "$PICK_PREFIX$sourceId-${LibraryTags.keyOf(it)}" }
                ?: "source-$sourceId"
    }

    private companion object {
        const val CONTINUE_READING_LIMIT = 15
        const val HOME_SOURCES = 4
        const val ROW_LIMIT = 18
        const val HOME_SAVED_SEARCHES = 6
        const val NEW_CHAPTER_DAYS = 14
        const val NEW_CHAPTERS_LIMIT = 20
        const val PICK_ROWS = 2
        const val PICK_TAG_CANDIDATES = 6
        const val PICK_MIN_COUNT = 2
    }
}

private const val SAVED_PREFIX = "saved-"
private const val PICK_PREFIX = "pick-"

private const val HERO_COUNT = 6
