package eu.kanade.tachiyomi.ui.browse.source.browse

import android.content.res.Configuration
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.core.preference.asState
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.util.removeCovers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kurotoon.catalogue.ActiveFacet
import kurotoon.catalogue.CatalogFacet
import kurotoon.catalogue.CatalogFacets
import kurotoon.catalogue.copyStateFrom
import mihon.core.metro.AppCoroutineScope
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.interactor.SetMangaDefaultChapterFlags
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetDuplicateLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.source.interactor.GetRemoteManga
import tachiyomi.domain.source.service.SourceManager
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import eu.kanade.tachiyomi.source.model.Filter as SourceModelFilter

@AssistedInject
class BrowseSourceViewModel(
    @Assisted private val sourceId: Long,
    @Assisted listingQuery: String?,
    @AppCoroutineScope private val appScope: CoroutineScope,
    private val sourceManager: SourceManager,
    sourcePreferences: SourcePreferences,
    private val libraryPreferences: LibraryPreferences,
    private val coverCache: CoverCache,
    private val getRemoteManga: GetRemoteManga,
    private val getDuplicateLibraryManga: GetDuplicateLibraryManga,
    private val getCategories: GetCategories,
    private val setMangaCategories: SetMangaCategories,
    private val setMangaDefaultChapterFlags: SetMangaDefaultChapterFlags,
    private val getManga: GetManga,
    private val updateManga: UpdateManga,
    private val addTracks: AddTracks,
    getIncognitoState: GetIncognitoState,
) : ViewModel() {

    val state: StateFlow<BrowseSourceViewModel.State>
        field = MutableStateFlow<BrowseSourceViewModel.State>(State(Listing.valueOf(listingQuery)))

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(sourceId: Long, listingQuery: String?): BrowseSourceViewModel
    }

    var displayMode by sourcePreferences.sourceDisplayMode.asState(viewModelScope)

    private val source: Source? get() = state.value.source

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val source = sourceManager.getOrStub(sourceId)

            state.update {
                var query: String? = null
                var listing = it.listing

                if (listing is Listing.Search) {
                    query = listing.query
                    listing = Listing.Search(query, source.getFilterList())
                }

                val defaults = source.getFilterList()
                it.copy(
                    source = source,
                    listing = listing,
                    filters = defaults,
                    toolbarQuery = query,
                    facets = CatalogFacets.from(defaults),
                    quickSorts = defaults.sortFilter()?.values?.toList().orEmpty(),
                )
            }

            if (!getIncognitoState.await(source.id)) {
                sourcePreferences.lastUsedSource.set(source.id)
            }
        }
    }

    /**
     * Flow of Pager flow tied to [State.listing]
     */
    private val hideInLibraryItems = sourcePreferences.hideInLibraryItems.get()
    val mangaPagerFlowFlow = state.map { it.source to it.listing }
        .filter { (source, _) -> source != null }
        .map { (_, listing) -> listing }
        .distinctUntilChanged()
        .map { listing ->
            Pager(PagingConfig(pageSize = 25)) {
                getRemoteManga(sourceId, listing.query ?: "", listing.filters)
            }.flow.map { pagingData ->
                pagingData.map { manga ->
                    getManga.subscribe(manga.url, manga.source)
                        .map { it ?: manga }
                        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), manga)
                }
                    .filter { !hideInLibraryItems || !it.value.favorite }
            }
                .cachedIn(viewModelScope)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyFlow())

    fun getColumnsPreference(orientation: Int): GridCells {
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
        val columns = if (isLandscape) {
            libraryPreferences.landscapeColumns
        } else {
            libraryPreferences.portraitColumns
        }.get()
        return if (columns == 0) GridCells.Adaptive(128.dp) else GridCells.Fixed(columns)
    }

    fun resetFilters() {
        val source = source ?: return
        state.update { it.copy(filters = source.getFilterList()) }
    }

    fun setListing(listing: Listing) {
        state.update { it.copy(listing = listing, toolbarQuery = null, selectedSort = null) }
    }

    /**
     * Edits the applied filters from the catalogue chips: starts from a fresh copy of the
     * source's filters carrying over what is applied now, so the chips always match the results.
     * Clearing everything goes back to the popular listing.
     */
    private fun editApplied(sort: Int? = state.value.selectedSort, edit: (FilterList) -> Unit) {
        val source = source ?: return
        val current = state.value
        val filters = source.getFilterList()
        current.appliedFilters?.let(filters::copyStateFrom)
        edit(filters)
        val query = (current.listing as? Listing.Search)?.query?.takeIf { it.isNotEmpty() }
        if (query == null && sort == null && CatalogFacets.active(current.facets, filters).isEmpty()) {
            resetFilters()
            setListing(Listing.Popular)
            return
        }
        state.update {
            it.copy(
                filters = filters,
                listing = Listing.Search(query = query, filters = filters),
                toolbarQuery = query,
                selectedSort = sort,
            )
        }
    }

    /** Picks one of the source's sort orders, keeping any catalogue chips that are applied. */
    fun applySort(sort: Int?) {
        editApplied(sort) { filters ->
            val filter = filters.sortFilter() ?: return@editApplied
            if (sort == null) {
                source?.getFilterList()?.sortFilter()?.let { filter.state = it.state }
            } else {
                val name = filter.values.getOrNull(sort).orEmpty()
                val ascending = AscendingSortHints.any { name.contains(it, ignoreCase = true) }
                filter.state = SourceModelFilter.Sort.Selection(sort, ascending)
            }
        }
    }

    /** Tap on a tag chip: include, then exclude when the source allows it, then off. */
    fun cycleTag(filterIndex: Int, childIndex: Int) {
        editApplied { CatalogFacets.cycleTag(it, filterIndex, childIndex) }
    }

    /** Long press on a tag chip: exclude it straight away. */
    fun excludeTag(filterIndex: Int, childIndex: Int) {
        editApplied { CatalogFacets.excludeTag(it, filterIndex, childIndex) }
    }

    fun selectChoice(filterIndex: Int, valueIndex: Int) {
        val facet = state.value.facets.firstOrNull { it.filterIndex == filterIndex } as? CatalogFacet.Choice
        editApplied { CatalogFacets.selectChoice(it, filterIndex, valueIndex, facet?.defaultIndex ?: 0) }
    }

    fun clearFacet(active: ActiveFacet) {
        val facet = state.value.facets.firstOrNull { it.filterIndex == active.filterIndex } as? CatalogFacet.Choice
        editApplied { CatalogFacets.clear(it, active.filterIndex, active.childIndex, facet?.defaultIndex ?: 0) }
    }

    /** Drops every catalogue chip but keeps the text query and sort. */
    fun clearFacets() {
        val source = source ?: return
        val defaults = source.getFilterList()
        editApplied { filters ->
            val sort = filters.sortFilter()?.state
            filters.copyStateFrom(defaults)
            filters.sortFilter()?.state = sort
        }
    }

    fun setFilters(filters: FilterList) {
        state.update {
            it.copy(
                filters = filters,
            )
        }
    }

    fun search(query: String? = null, filters: FilterList? = null) {
        val input = state.value.listing as? Listing.Search
            ?: Listing.Search(query = null, filters = source?.getFilterList() ?: FilterList())

        state.update {
            it.copy(
                listing = input.copy(
                    query = query ?: input.query,
                    filters = filters ?: input.filters,
                ),
                toolbarQuery = query ?: input.query,
                selectedSort = if (filters != null) null else it.selectedSort,
            )
        }
    }

    fun searchGenre(genreName: String) {
        val defaultFilters = source?.getFilterList() ?: return
        var genreExists = false

        filter@ for (sourceFilter in defaultFilters) {
            if (sourceFilter is SourceModelFilter.Group<*>) {
                for (filter in sourceFilter.state) {
                    if (filter is SourceModelFilter<*> && filter.name.equals(genreName, true)) {
                        when (filter) {
                            is SourceModelFilter.TriState -> filter.state = 1
                            is SourceModelFilter.CheckBox -> filter.state = true
                            else -> {}
                        }
                        genreExists = true
                        break@filter
                    }
                }
            } else if (sourceFilter is SourceModelFilter.Select<*>) {
                val index = sourceFilter.values.filterIsInstance<String>()
                    .indexOfFirst { it.equals(genreName, true) }

                if (index != -1) {
                    sourceFilter.state = index
                    genreExists = true
                    break
                }
            }
        }

        state.update {
            val listing = if (genreExists) {
                Listing.Search(query = null, filters = defaultFilters)
            } else {
                Listing.Search(query = genreName, filters = defaultFilters)
            }
            it.copy(
                filters = defaultFilters,
                listing = listing,
                toolbarQuery = listing.query,
                selectedSort = null,
            )
        }
    }

    /**
     * Adds or removes a manga from the library.
     *
     * @param manga the manga to update.
     */
    fun changeMangaFavorite(manga: Manga) {
        appScope.launch {
            val update = if (manga.favorite) {
                manga.removeCovers(coverCache)
                MangaUpdate(manga.id) { favoriteAt = null }
            } else {
                setMangaDefaultChapterFlags.await(manga)
                addTracks.bindEnhancedTrackers(manga, sourceManager.getOrStub(manga.source))
                MangaUpdate(manga.id) { favoriteAt = Clock.System.now() }
            }

            updateManga.await(update)
        }
    }

    fun addFavorite(manga: Manga) {
        viewModelScope.launch {
            val categories = getCategories()
            val defaultCategoryId = libraryPreferences.defaultCategory.get()
            val defaultCategory = categories.find { it.id == defaultCategoryId.toLong() }

            when {
                // Default category set
                defaultCategory != null -> {
                    moveMangaToCategories(manga, defaultCategory)

                    changeMangaFavorite(manga)
                }

                // Automatic 'Default' or no categories
                defaultCategoryId == 0 || categories.isEmpty() -> {
                    moveMangaToCategories(manga)

                    changeMangaFavorite(manga)
                }

                // Choose a category
                else -> {
                    val preselectedIds = getCategories.await(manga.id).map { it.id }
                    setDialog(
                        Dialog.ChangeMangaCategory(
                            manga,
                            categories.mapAsCheckboxState { it.id in preselectedIds },
                        ),
                    )
                }
            }
        }
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    suspend fun getCategories(): List<Category> {
        return getCategories.subscribe()
            .firstOrNull()
            ?.filterNot { it.isSystemCategory }
            .orEmpty()
    }

    suspend fun getDuplicateLibraryManga(manga: Manga): List<MangaWithChapterCount> {
        return getDuplicateLibraryManga.invoke(manga)
    }

    private fun moveMangaToCategories(manga: Manga, vararg categories: Category) {
        moveMangaToCategories(manga, categories.filter { it.id != 0L }.map { it.id })
    }

    fun moveMangaToCategories(manga: Manga, categoryIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            setMangaCategories.await(
                mangaId = manga.id,
                categoryIds = categoryIds.toList(),
            )
        }
    }

    fun openFilterSheet() {
        setDialog(Dialog.Filter)
    }

    fun setDialog(dialog: Dialog?) {
        state.update { it.copy(dialog = dialog) }
    }

    fun setToolbarQuery(query: String?) {
        state.update { it.copy(toolbarQuery = query) }
    }

    sealed class Listing(open val query: String?, open val filters: FilterList) {
        data object Popular : Listing(query = GetRemoteManga.QUERY_POPULAR, filters = FilterList())
        data object Latest : Listing(query = GetRemoteManga.QUERY_LATEST, filters = FilterList())
        data class Search(
            override val query: String?,
            override val filters: FilterList,
        ) : Listing(query = query, filters = filters)

        companion object {
            fun valueOf(query: String?): Listing {
                return when (query) {
                    GetRemoteManga.QUERY_POPULAR -> Popular
                    GetRemoteManga.QUERY_LATEST -> Latest
                    else -> Search(query = query, filters = FilterList()) // filters are filled in later
                }
            }
        }
    }

    sealed interface Dialog {
        data object Filter : Dialog
        data class RemoveManga(val manga: Manga) : Dialog
        data class AddDuplicateManga(val manga: Manga, val duplicates: List<MangaWithChapterCount>) : Dialog
        data class ChangeMangaCategory(
            val manga: Manga,
            val initialSelection: List<CheckboxState.State<Category>>,
        ) : Dialog
        data class Migrate(val target: Manga, val current: Manga) : Dialog
    }

    @Immutable
    data class State(
        val listing: Listing,
        val source: Source? = null,
        val filters: FilterList = FilterList(),
        val toolbarQuery: String? = null,
        val dialog: Dialog? = null,
        val facets: List<CatalogFacet> = emptyList(),
        val quickSorts: List<String> = emptyList(),
        val selectedSort: Int? = null,
    ) {
        val isUserQuery get() = listing is Listing.Search && !listing.query.isNullOrEmpty()

        /** The filters behind the results on screen, or null for the popular and latest feeds. */
        val appliedFilters: FilterList? get() = (listing as? Listing.Search)?.filters?.takeIf { it.isNotEmpty() }

        val activeFacets: List<ActiveFacet> get() = appliedFilters?.let { CatalogFacets.active(facets, it) }.orEmpty()
    }
}

/** Sort names that read best A to Z or oldest first. */
private val AscendingSortHints = listOf("title", "name", "alphabet", "a-z")

private fun FilterList.sortFilter(): SourceModelFilter.Sort? =
    firstNotNullOfOrNull { it as? SourceModelFilter.Sort }
