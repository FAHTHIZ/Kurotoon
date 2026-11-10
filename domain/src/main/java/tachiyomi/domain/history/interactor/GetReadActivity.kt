package tachiyomi.domain.history.interactor

import dev.zacsweers.metro.Inject
import tachiyomi.domain.history.repository.HistoryRepository
import kotlin.time.Instant

@Inject
class GetReadActivity(
    private val repository: HistoryRepository,
) {

    suspend fun await(since: Instant): List<Pair<Instant, Long>> {
        return repository.getReadActivity(since)
    }
}
