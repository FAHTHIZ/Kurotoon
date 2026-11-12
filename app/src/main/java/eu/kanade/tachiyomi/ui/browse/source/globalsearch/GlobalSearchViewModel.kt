package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.Source
import kurotoon.catalogue.RecentSearches
import kurotoon.prefs.KuroPreferences
import kurotoon.rewards.RewardEvent
import kurotoon.rewards.RewardsStore
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.source.service.SourceManager

@AssistedInject
class GlobalSearchViewModel(
    @Assisted initialQuery: String,
    @Assisted initialExtensionFilter: String?,
    sourcePreferences: SourcePreferences,
    sourceManager: SourceManager,
    extensionManager: ExtensionManager,
    networkToLocalManga: NetworkToLocalManga,
    getManga: GetManga,
    preferences: SourcePreferences,
    private val rewardsStore: RewardsStore,
    private val kuroPreferences: KuroPreferences,
) : SearchViewModel(
    // Pinned-only is a fine default once you've pinned something; before that it would search nothing at all
    initialState = State(
        searchQuery = initialQuery,
        sourceFilter = if (sourcePreferences.pinnedSources.get().isEmpty()) {
            SourceFilter.All
        } else {
            SourceFilter.PinnedOnly
        },
    ),
    sourcePreferences = sourcePreferences,
    sourceManager = sourceManager,
    extensionManager = extensionManager,
    networkToLocalManga = networkToLocalManga,
    getManga = getManga,
    preferences = preferences,
) {

    override fun hiddenSeriesIds(): Set<String> = kuroPreferences.hiddenSeries.get()

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(initialQuery: String, initialExtensionFilter: String?): GlobalSearchViewModel
    }

    init {
        extensionFilter = initialExtensionFilter
        if (initialQuery.isNotBlank() || !initialExtensionFilter.isNullOrBlank()) {
            if (extensionFilter != null) {
                // we're going to use custom extension filter instead
                setSourceFilter(SourceFilter.All)
            }
            search()
        }
    }

    val recentSearches = kuroPreferences.recentSearches.changes()

    fun clearRecentSearches() {
        kuroPreferences.recentSearches.delete()
    }

    fun searchRecent(query: String) {
        updateSearchQuery(query)
        search()
    }

    override fun onSearch() {
        rewardsStore.track(RewardEvent.SEARCH)
        state.value.searchQuery?.let { query ->
            kuroPreferences.recentSearches.getAndSet { RecentSearches.add(it, query) }
        }
    }

    override suspend fun getEnabledSources(): List<Source> {
        return super.getEnabledSources()
            .filter { state.value.sourceFilter != SourceFilter.PinnedOnly || "${it.id}" in pinnedSources }
    }
}
