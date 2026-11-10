package kurotoon.stats

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.time.Instant

/** Chapters finished on one calendar day and the time spent on them. */
data class ReadingDay(val date: LocalDate, val chapters: Int, val millis: Long)

/**
 * A calendar of reading over the last [weeks] weeks, Monday-first, ending with the week of today.
 * Built from history, so a chapter counts on the day it was last opened.
 */
data class ReadingActivity(
    val weeks: List<List<ReadingDay?>>,
    val totalChapters: Int,
    val activeDays: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val busiest: Int,
    /** Chapters read so far today. */
    val today: Int = 0,
) {
    companion object {
        const val WEEKS = 17

        fun startOf(today: LocalDate, weeks: Int = WEEKS): LocalDate =
            today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks((weeks - 1).toLong())

        fun build(
            entries: List<Pair<Instant, Long>>,
            today: LocalDate,
            zone: ZoneId,
            weeks: Int = WEEKS,
        ): ReadingActivity {
            val start = startOf(today, weeks)
            val byDay = entries
                .groupBy { (at, _) ->
                    java.time.Instant.ofEpochMilli(at.toEpochMilliseconds()).atZone(zone).toLocalDate()
                }
                .mapValues { (_, list) -> list.size to list.sumOf { it.second } }

            val grid = (0 until weeks).map { w ->
                (0 until 7).map { d ->
                    val date = start.plusDays((w * 7 + d).toLong())
                    if (date.isAfter(today)) {
                        null
                    } else {
                        val (count, millis) = byDay[date] ?: (0 to 0L)
                        ReadingDay(date, count, millis)
                    }
                }
            }
            val days = grid.flatten().filterNotNull()

            var longest = 0
            var run = 0
            for (day in days) {
                run = if (day.chapters > 0) run + 1 else 0
                longest = maxOf(longest, run)
            }
            // Today still counts towards the streak until it ends without reading
            var current = 0
            var cursor = if ((byDay[today]?.first ?: 0) > 0) today else today.minusDays(1)
            while ((byDay[cursor]?.first ?: 0) > 0 && !cursor.isBefore(start)) {
                current++
                cursor = cursor.minusDays(1)
            }

            return ReadingActivity(
                weeks = grid,
                totalChapters = days.sumOf { it.chapters },
                activeDays = days.count { it.chapters > 0 },
                currentStreak = current,
                longestStreak = longest,
                busiest = days.maxOfOrNull { it.chapters } ?: 0,
                today = byDay[today]?.first ?: 0,
            )
        }
    }
}
