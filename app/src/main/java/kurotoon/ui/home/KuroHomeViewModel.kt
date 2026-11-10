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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.source.local.LocalSource

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class KuroHomeViewModel(
    getHistory: GetHistory,
    getEnabledSources: GetEnabledSources,
    private val getNextChapters: GetNextChapters,
) : ViewModel() {

    private val openChapterEvents = Channel<Chapter?>(Channel.UNLIMITED)

    /** The chapter to open after tapping "continue"; null when the series has nothing left. */
    val openChapter: Flow<Chapter?> = openChapterEvents.receiveAsFlow()

    val state: StateFlow<State> = combine(
        getHistory.subscribe("").catch { logcat(LogPriority.ERROR, it) },
        getEnabledSources.subscribe().catch { logcat(LogPriority.ERROR, it) },
    ) { history, sources ->
        State(
            isLoading = false,
            continueReading = history.distinctBy { it.mangaId }.take(CONTINUE_READING_LIMIT),
            hasSources = sources.any { it.id != LocalSource.ID },
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())

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
    )

    private companion object {
        const val CONTINUE_READING_LIMIT = 15
    }
}
