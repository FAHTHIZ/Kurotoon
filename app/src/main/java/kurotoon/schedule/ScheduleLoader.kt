package kurotoon.schedule

import androidx.compose.runtime.Immutable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.interactor.GetLibraryManga
import kotlin.time.Clock

/** One library series placed on the weekly schedule. */
@Immutable
data class ScheduledSeries(
    val item: LibraryManga,
    val days: List<DayOfWeek>,
    val latest: LocalDate?,
) {
    /** Days since the last chapter, or null when no chapter has a date. */
    fun daysSinceLatest(today: LocalDate): Int? = latest?.daysUntil(today)
}

@Immutable
data class WeeklySchedule(
    val today: LocalDate,
    val byDay: Map<DayOfWeek, List<ScheduledSeries>>,
    /** Ongoing series with no steady weekday. */
    val unscheduled: Int,
) {
    fun on(day: DayOfWeek): List<ScheduledSeries> = byDay[day].orEmpty()

    /** Series that usually update today, the freshest first. */
    val dueToday: List<ScheduledSeries> get() = on(today.dayOfWeek)

    companion object {
        val EMPTY = WeeklySchedule(LocalDate(1970, 1, 1), emptyMap(), 0)
    }
}

/** Builds the weekly schedule of ongoing library series from their chapter upload dates. */
@Inject
@SingleIn(AppScope::class)
class ScheduleLoader(
    private val getLibraryManga: GetLibraryManga,
    private val chapterRepository: ChapterRepository,
) {

    fun subscribe(): Flow<WeeklySchedule> = getLibraryManga.subscribe()
        .map { library -> build(library) }
        .flowOn(Dispatchers.IO)
        .catch { logcat(LogPriority.ERROR, it) }
        .distinctUntilChanged()

    private suspend fun build(library: List<LibraryManga>): WeeklySchedule {
        val zone = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(zone)
        val ongoing = library.distinctBy { it.id }.filter { it.manga.status !in FINISHED }
        if (ongoing.isEmpty()) return WeeklySchedule(today, emptyMap(), 0)
        val chapters = ongoing.map { it.id }.chunked(CHUNK)
            .flatMap { chapterRepository.getChaptersByMangaIds(it).entries }
            .associate { it.key to it.value }
        var unscheduled = 0
        val placed = ongoing.mapNotNull { item ->
            val uploads = chapters[item.id].orEmpty().mapNotNull { it.dateUpload }
            val days = ReleaseSchedule.weekdays(uploads, today, zone)
            if (days.isEmpty()) {
                unscheduled++
                null
            } else {
                ScheduledSeries(item, days, ReleaseSchedule.latest(uploads, zone))
            }
        }
        val byDay = DayOfWeek.entries.associateWith { day ->
            placed.filter { day in it.days }
                .sortedWith(
                    compareByDescending<ScheduledSeries> { it.latest }
                        .thenBy { it.item.manga.title.lowercase() },
                )
        }
        return WeeklySchedule(today, byDay, unscheduled)
    }

    private companion object {
        const val CHUNK = 200
        val FINISHED = setOf(
            SManga.COMPLETED.toLong(),
            SManga.PUBLISHING_FINISHED.toLong(),
            SManga.CANCELLED.toLong(),
        )
    }
}
