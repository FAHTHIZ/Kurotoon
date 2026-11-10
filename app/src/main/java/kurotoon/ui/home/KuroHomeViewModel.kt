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
) : ViewModel() {

    private val rows = MutableStateFlow<List<SourceRow>>(emptyList())
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
    ) { history, ids, rows ->
        State(
            isLoading = false,
            continueReading = history.distinctBy { it.mangaId }.take(CONTINUE_READING_LIMIT),
            hasSources = ids.isNotEmpty(),
            rows = rows,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())

    init {
        viewModelScope.launch {
            sourceIds.collectLatest { ids -> loadRows(ids) }
        }
    }

    /** Fetches each source's popular list, like the web app's home rows. */
    private suspend fun loadRows(ids: List<Long>) = coroutineScope {
        rows.value = ids.mapNotNull { id ->
            sourceManager.get(id)?.let { SourceRow(it.id, it.name, items = null) }
        }
        rows.value.forEach { row ->
            launch { loadRow(row.sourceId) }
        }
    }

    fun retry(sourceId: Long) {
        viewModelScope.launch { loadRow(sourceId) }
    }

    private suspend fun loadRow(sourceId: Long) {
        val source = sourceManager.get(sourceId) ?: return
        updateRow(sourceId) { it.copy(items = null, failed = false) }
        val result = runCatching {
            val page = withContext(Dispatchers.IO) { source.getPopularManga(1) }
            page.mangas
                .map { it.toDomainManga(source.id) }
                .distinctBy { it.url }
                .let { networkToLocalManga(it) }
                .take(ROW_LIMIT)
        }
        result.exceptionOrNull()?.let { logcat(LogPriority.WARN, it) { "Home row failed for ${source.name}" } }
        updateRow(sourceId) { it.copy(items = result.getOrNull(), failed = result.isFailure) }
    }

    private fun updateRow(sourceId: Long, block: (SourceRow) -> SourceRow) {
        rows.update { list -> list.map { if (it.sourceId == sourceId) block(it) else it } }
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
    ) {
        /** The first source's top title, shown as the hero card. */
        val hero: Pair<SourceRow, Manga>?
            get() = rows.firstOrNull()?.let { row ->
                row.items?.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.let { row to it }
            }
    }

    @Immutable
    data class SourceRow(
        val sourceId: Long,
        val name: String,
        /** Null while loading. */
        val items: List<Manga>?,
        val failed: Boolean = false,
    )

    private companion object {
        const val CONTINUE_READING_LIMIT = 15
        const val HOME_SOURCES = 4
        const val ROW_LIMIT = 18
    }
}
