package kurotoon.schedule

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Works out which weekdays a series usually gets a chapter on, the way Webtoon's daily schedule
 * lists each series under its update day.
 *
 * Only the [RECENT_DATES] most recent upload dates count, so a series that changed its day
 * follows the new one. Several chapters on one day count once. A weekday needs at least
 * [MIN_HITS] uploads and [MIN_SHARE] of them to make the schedule, and a series that hasn't
 * posted for [STALE_DAYS] is treated as on break.
 */
object ReleaseSchedule {

    const val RECENT_DATES = 10
    const val MIN_DATES = 3
    const val MIN_HITS = 2
    const val MIN_SHARE = 0.3
    const val MAX_DAYS = 3
    const val STALE_DAYS = 45

    /** The weekdays this series updates on, earliest in the week first; empty when there's no pattern. */
    fun weekdays(uploads: List<Instant>, today: LocalDate, zone: TimeZone): List<DayOfWeek> {
        val dates = recentDates(uploads, zone)
        if (dates.size < MIN_DATES) return emptyList()
        if (dates.first().daysUntil(today) > STALE_DAYS) return emptyList()
        val counts = dates.groupingBy { it.dayOfWeek }.eachCount()
        val days = counts
            .filter { (_, hits) -> hits >= MIN_HITS && hits.toDouble() / dates.size >= MIN_SHARE }
            .keys
            .sorted()
        // More than a few days a week is "daily", which is noise rather than a schedule
        return if (days.size in 1..MAX_DAYS) days else emptyList()
    }

    /** Distinct local upload dates, newest first. */
    fun recentDates(uploads: List<Instant>, zone: TimeZone): List<LocalDate> =
        uploads.asSequence()
            .filter { it.toEpochMilliseconds() > 0 }
            .map { it.toLocalDateTime(zone).date }
            .distinct()
            .sortedDescending()
            .take(RECENT_DATES)
            .toList()

    /** The newest upload date, or null when none has a date. */
    fun latest(uploads: List<Instant>, zone: TimeZone): LocalDate? = recentDates(uploads, zone).firstOrNull()
}
