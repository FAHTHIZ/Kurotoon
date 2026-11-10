package tachiyomi.data.history

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.history.repository.HistoryRepository
import kotlin.time.Instant

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class HistoryRepositoryImpl(
    private val database: Database,
) : HistoryRepository {

    override fun getHistory(query: String): Flow<List<HistoryWithRelations>> {
        return database.historyViewQueries
            .history(query, HistoryMapper::mapHistoryWithRelations)
            .subscribeToList()
    }

    override suspend fun getLastHistory(): HistoryWithRelations? {
        return database.historyViewQueries
            .getLatestHistory(HistoryMapper::mapHistoryWithRelations)
            .awaitAsOneOrNull()
    }

    override suspend fun getTotalReadDuration(): Long {
        return database.historyQueries
            .getReadDuration()
            .awaitAsOne()
    }

    override suspend fun getReadActivity(since: Instant): List<Pair<Instant, Long>> {
        return database.historyQueries
            .getReadActivitySince(since)
            .awaitAsList()
            .mapNotNull { row -> row.read_at?.let { it to row.read_duration } }
    }

    override suspend fun getReadEntries(since: Instant): List<tachiyomi.domain.history.model.ReadEntry> {
        return database.historyQueries
            .getReadEntriesSince(since)
            .awaitAsList()
            .mapNotNull { row ->
                val readAt = row.read_at ?: return@mapNotNull null
                tachiyomi.domain.history.model.ReadEntry(
                    readAt = readAt,
                    duration = row.read_duration,
                    mangaId = row.manga_id,
                    title = row.remote_title,
                    genres = row.remote_genre.orEmpty(),
                )
            }
    }

    override suspend fun getHistoryByMangaId(mangaId: Long): List<History> {
        return database.historyQueries
            .getHistoryByMangaId(mangaId, HistoryMapper::mapHistory)
            .awaitAsList()
    }

    override suspend fun getHistoryByMangaIds(mangaIds: List<Long>): Map<Long, List<History>> {
        return database.historyQueries
            .getHistoryByMangaIds(mangaIds) { mangaId, id, chapterId, readAt, readDuration ->
                mangaId to HistoryMapper.mapHistory(id, chapterId, readAt, readDuration)
            }
            .awaitAsList()
            .groupBy({ it.first }, { it.second })
    }

    override suspend fun resetHistory(historyId: Long) {
        try {
            database.historyQueries.resetHistoryById(historyId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun resetHistoryByMangaId(mangaId: Long) {
        try {
            database.historyQueries.resetHistoryByMangaId(mangaId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun deleteAllHistory(): Boolean {
        return try {
            database.historyQueries.removeAllHistory()
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
            false
        }
    }

    override suspend fun deleteResetHistory() {
        database.historyQueries.removeResettedHistory()
    }

    override suspend fun upsertHistory(historyUpdate: HistoryUpdate) {
        try {
            database.historyQueries.upsert(
                chapterId = historyUpdate.chapterId,
                readAt = historyUpdate.readAt,
                readDuration = historyUpdate.sessionReadDuration,
            )
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }
}
