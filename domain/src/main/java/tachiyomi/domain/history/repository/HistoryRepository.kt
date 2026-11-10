package tachiyomi.domain.history.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.model.HistoryWithRelations
import kotlin.time.Instant

interface HistoryRepository {

    fun getHistory(query: String): Flow<List<HistoryWithRelations>>

    suspend fun getLastHistory(): HistoryWithRelations?

    suspend fun getTotalReadDuration(): Long

    /** When each chapter read since [since] was last read, with its total read time in milliseconds. */
    suspend fun getReadActivity(since: Instant): List<Pair<Instant, Long>>
    suspend fun getReadEntries(since: Instant): List<tachiyomi.domain.history.model.ReadEntry>

    suspend fun getHistoryByMangaId(mangaId: Long): List<History>

    suspend fun getHistoryByMangaIds(mangaIds: List<Long>): Map<Long, List<History>>

    suspend fun resetHistory(historyId: Long)

    suspend fun resetHistoryByMangaId(mangaId: Long)

    suspend fun deleteAllHistory(): Boolean

    suspend fun deleteResetHistory()

    suspend fun upsertHistory(historyUpdate: HistoryUpdate)
}
