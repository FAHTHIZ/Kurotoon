package kurotoon.ui.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kurotoon.catalogue.CatalogFacets
import kurotoon.catalogue.HiddenTags
import kurotoon.prefs.KuroPreferences
import kurotoon.ui.home.KuroHomeViewModel.SourceRow
import logcat.LogPriority
import mihon.domain.manga.model.toDomainManga
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.LocalSource

/**
 * One tag browsed on every enabled source that offers it as a filter, a row per source. Text
 * search only matches titles; this uses each catalogue's own genre filter instead.
 */
@AssistedInject
class TagEverywhereViewModel(
    @Assisted val tag: String,
    private val getEnabledSources: GetEnabledSources,
    private val sourceManager: SourceManager,
    private val networkToLocalManga: NetworkToLocalManga,
    private val kuroPreferences: KuroPreferences,
) : ViewModel() {

    /** Null while finding which sources have the tag. */
    val rows: StateFlow<List<SourceRow>?>
        field = MutableStateFlow<List<SourceRow>?>(null)

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val sources = runCatching { getEnabledSources.subscribe().first() }
            .onFailure { logcat(LogPriority.ERROR, it) }
            .getOrDefault(emptyList())
            .filterNot { it.id == LocalSource.ID }
            .mapNotNull { sourceManager.get(it.id) as? CatalogueSource }
        val matching = withContext(Dispatchers.Default) {
            sources.filter { source ->
                runCatching { CatalogFacets.includeTag(source.getFilterList(), tag) }.getOrDefault(false)
            }
        }
        rows.value = matching.map { SourceRow(it.id, it.name, items = null) }
        val permits = Semaphore(PARALLEL)
        matching.forEach { source ->
            viewModelScope.launch { permits.withPermit { loadRow(source) } }
        }
    }

    fun retry(row: SourceRow) {
        viewModelScope.launch {
            (sourceManager.get(row.sourceId) as? CatalogueSource)?.let { loadRow(it) }
        }
    }

    private suspend fun loadRow(source: CatalogueSource) {
        update(source.id) { it.copy(items = null, failed = false) }
        val hidden = HiddenTags.parse(kuroPreferences.hiddenTags.get())
        val result = runCatching {
            val page = withContext(Dispatchers.IO) {
                val filters = source.getFilterList()
                CatalogFacets.includeTag(filters, tag)
                HiddenTags.excludeIn(filters, hidden)
                source.getSearchManga(1, "", filters)
            }
            page.mangas
                .map { it.toDomainManga(source.id) }
                .distinctBy { it.url }
                .let { networkToLocalManga(it) }
                .filterNot { HiddenTags.hides(it.genre, hidden) }
                .take(ROW_LIMIT)
        }
        result.exceptionOrNull()?.let { logcat(LogPriority.WARN, it) { "Tag row failed for ${source.name}" } }
        update(source.id) { it.copy(items = result.getOrNull(), failed = result.isFailure) }
    }

    private fun update(sourceId: Long, block: (SourceRow) -> SourceRow) {
        rows.update { list -> list?.map { if (it.sourceId == sourceId) block(it) else it } }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(tag: String): TagEverywhereViewModel
    }

    companion object {
        private const val PARALLEL = 4

        /** Sources with titles first, then ones still loading, then failures, then empty ones. */
        fun ordered(rows: List<SourceRow>): List<SourceRow> = rows.sortedBy { row ->
            when {
                !row.items.isNullOrEmpty() -> 0
                row.items == null && !row.failed -> 1
                row.failed -> 2
                else -> 3
            }
        }

        private const val ROW_LIMIT = 18
    }
}
